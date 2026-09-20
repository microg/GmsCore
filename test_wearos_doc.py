import os
import unittest


class WearOSDocTest(unittest.TestCase):
    def test_wearos_plan_exists_and_has_sections(self):
        repo_root = os.path.dirname(os.path.abspath(__file__))
        doc_path = os.path.join(repo_root, "docs", "wearos_support.md")
        self.assertTrue(os.path.exists(doc_path), "docs/wearos_support.md is missing")

        with open(doc_path, encoding="utf-8") as f:
            content = f.read()

        required_sections = [
            "WearOS Support Plan",
            "Pairing",
            "Notifications",
            "Media",
            "App support",
            "Testing matrix",
            "Next steps",
        ]
        for section in required_sections:
            self.assertIn(section, content, f"Section '{section}' missing from wearOS plan")

        self.assertGreater(len(content.splitlines()), 10, "Document should have meaningful content")


if __name__ == "__main__":
    unittest.main()
