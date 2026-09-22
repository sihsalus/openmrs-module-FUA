import importlib.util
from pathlib import Path
import tempfile
import unittest
from zipfile import ZipFile


spec = importlib.util.spec_from_file_location(
    "prepare_backend_smoke", Path(__file__).with_name("prepare-backend-smoke.py")
)
smoke = importlib.util.module_from_spec(spec)
spec.loader.exec_module(smoke)


class BackendSmokePreparationTest(unittest.TestCase):
    def setUp(self):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        self.root = Path(directory.name)
        (self.root / "backend").mkdir()
        self.pom = self.root / "backend/pom.xml"
        self.dockerfile = self.root / "backend/Dockerfile"
        self.pom.write_text("<project><fua.version>1.0.89</fua.version></project>")
        self.dockerfile.write_text(
            "COPY backend ./backend/\n"
            "RUN --mount=type=cache,target=/root/.m2/repository \\\n"
            "    bash backend/bin/build-source-omods.sh package && \\\n"
            "    mvn $MVN_ARGS_SETTINGS $MVN_ARGS && \\\n"
            "    verify-packaged-modules\n"
        )
        self.module = self.root / "candidate.omod"
        self.write_module()

    def write_module(self, module_id="fua", version="1.0.90"):
        with ZipFile(self.module, "w") as archive:
            archive.writestr(
                "config.xml", f"<module><id>{module_id}</id><version>{version}</version></module>"
            )

    def assert_rejected_without_changes(self):
        original = (self.pom.read_bytes(), self.dockerfile.read_bytes())
        with self.assertRaises(ValueError):
            smoke.prepare(self.root, self.module)
        self.assertEqual(original, (self.pom.read_bytes(), self.dockerfile.read_bytes()))
        self.assertFalse((self.root / "backend/fua-smoke.omod").exists())

    def test_package_uses_exact_candidate_before_distro_build(self):
        smoke.prepare(self.root, self.module)
        self.assertIn("<fua.version>1.0.90</fua.version>", self.pom.read_text())
        self.assertEqual(self.module.read_bytes(), (self.root / "backend/fua-smoke.omod").read_bytes())
        dockerfile = self.dockerfile.read_text()
        self.assertLess(dockerfile.index("-DartifactId=fua-omod"), dockerfile.index("mvn $MVN_ARGS_SETTINGS"))
        self.assertIn("verify-packaged-modules", dockerfile)

    def test_rejects_another_module(self):
        self.write_module(module_id="another")
        self.assert_rejected_without_changes()

    def test_rejects_shell_content_in_version(self):
        self.write_module(version="1.0.90;echo unexpected")
        self.assert_rejected_without_changes()

    def test_rejects_ambiguous_version_pin(self):
        self.pom.write_text(self.pom.read_text() * 2)
        self.assert_rejected_without_changes()

    def test_rejects_changed_build_contract(self):
        self.dockerfile.write_text(self.dockerfile.read_text().replace("mvn $MVN_ARGS_SETTINGS", "mvn -B"))
        self.assert_rejected_without_changes()

    def test_rejects_repeated_preparation(self):
        smoke.prepare(self.root, self.module)
        original = (self.pom.read_bytes(), self.dockerfile.read_bytes())
        with self.assertRaises(ValueError):
            smoke.prepare(self.root, self.module)
        self.assertEqual(original, (self.pom.read_bytes(), self.dockerfile.read_bytes()))


if __name__ == "__main__":
    unittest.main()
