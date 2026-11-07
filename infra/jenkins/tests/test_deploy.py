import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / 'deploy-app.sh'
MOCK = r'''#!/usr/bin/env python3
import json, os, sys
tool = os.path.basename(sys.argv[0])
args = sys.argv[1:]
with open(os.environ['COMMAND_LOG'], 'a') as handle:
    handle.write(json.dumps([tool, *args]) + '\n')
if tool == 'aws' and args[:2] == ['sts', 'get-caller-identity']:
    print(os.environ.get('MOCK_ACCOUNT', '022868553365'))
elif tool == 'aws' and args[:2] == ['ecr', 'get-login-password']:
    print('test-token')
elif tool == 'gcloud' and args[:2] == ['auth', 'print-access-token']:
    print('test-token')
elif tool == 'helm' and args[0] == 'status':
    sys.exit(int(os.environ.get('MOCK_HELM_STATUS', '0')))
elif tool == 'helm' and args[:2] == ['get', 'values']:
    print(json.dumps({'backend': {'env': {'cloudProvider': os.environ['MOCK_CLOUD']}}}))
elif tool == 'docker' and args[0] == 'login':
    sys.stdin.read()
'''


class DeploymentTest(unittest.TestCase):
    def run_script(self, cloud, action, **overrides):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            binary = root / 'bin'
            binary.mkdir()
            for tool in ('aws', 'gcloud', 'kubectl', 'docker', 'helm'):
                path = binary / tool
                path.write_text(MOCK)
                path.chmod(0o755)
            log = root / 'commands.jsonl'
            env = dict(os.environ, PATH=f'{binary}:{os.environ["PATH"]}',
                       COMMAND_LOG=str(log), MOCK_CLOUD=cloud,
                       IMAGE_TAG='abcdef123456-7', AWS_ACCOUNT_ID='022868553365',
                       AWS_REGION='ap-northeast-2', GCP_PROJECT_ID='photo-storage-503901',
                       GCP_REGION='asia-northeast3', AWS_SHARED_CREDENTIALS_FILE='/unused',
                       GCP_CREDENTIALS_FILE='/unused')
            env.update(overrides)
            result = subprocess.run(['bash', str(SCRIPT), cloud, action], env=env,
                                    capture_output=True, text=True)
            calls = [json.loads(line) for line in log.read_text().splitlines()] if log.exists() else []
            return result, calls

    def test_wrong_aws_account_cannot_publish(self):
        result, calls = self.run_script('aws', 'publish', MOCK_ACCOUNT='000000000000')
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(any(call[0] == 'docker' for call in calls))

    def test_missing_release_cannot_publish(self):
        result, calls = self.run_script('gcp', 'publish', MOCK_HELM_STATUS='1')
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(any(call[0] == 'docker' for call in calls))

    def test_wrong_release_cloud_cannot_deploy(self):
        result, calls = self.run_script('gcp', 'deploy', MOCK_CLOUD='aws')
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(any(call[:2] == ['helm', 'upgrade'] for call in calls))

    def test_publish_pushes_both_versioned_images(self):
        for cloud in ('aws', 'gcp'):
            with self.subTest(cloud=cloud):
                result, calls = self.run_script(cloud, 'publish')
                self.assertEqual(result.returncode, 0, result.stderr)
                pushes = [call[2] for call in calls if call[:2] == ['docker', 'push']]
                self.assertEqual(len(pushes), 2)
                self.assertTrue(all(image.endswith(':abcdef123456-7') for image in pushes))
                self.assertFalse(any(call[:2] == ['helm', 'upgrade'] for call in calls))

    def test_deploy_preserves_values_and_waits_with_rollback(self):
        for cloud in ('aws', 'gcp'):
            with self.subTest(cloud=cloud):
                result, calls = self.run_script(cloud, 'deploy')
                self.assertEqual(result.returncode, 0, result.stderr)
                upgrade = next(call for call in calls if call[:2] == ['helm', 'upgrade'])
                for argument in ('--reuse-values', '--atomic', '--wait', 'backend.probes.enabled=true'):
                    self.assertIn(argument, upgrade)
                self.assertNotIn('--install', upgrade)
                self.assertIn('frontend.image.tag=abcdef123456-7', upgrade)
                rollouts = [call for call in calls if 'rollout' in call]
                self.assertEqual(len(rollouts), 2)


if __name__ == '__main__':
    unittest.main()
