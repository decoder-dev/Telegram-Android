import importlib.util
import io
from pathlib import Path
import struct
import tempfile
import unittest
from zipfile import ZipFile

spec = importlib.util.spec_from_file_location('native_pages', Path(__file__).with_name('check-apk-native-pages.py'))
native = importlib.util.module_from_spec(spec)
spec.loader.exec_module(native)


def elf(alignment):
    data = bytearray(120)
    data[:6] = b'\x7fELF\x02\x01'
    struct.pack_into('<Q', data, 32, 64)
    struct.pack_into('<HH', data, 54, 56, 1)
    struct.pack_into('<I', data, 64, 1)
    struct.pack_into('<Q', data, 112, alignment)
    return data


class NativePagesTest(unittest.TestCase):
    def test_accepts_16k_and_rejects_4k_and_invalid_headers(self):
        native.check_elf(elf(16384), 'lib.so')
        for value in (elf(4096), b'PK\x03\x04', elf(16384)[:64]):
            with self.assertRaises(ValueError): native.check_elf(value, 'lib.so')

    def archive(self, replacement=None, dependency='libwebp.so'):
        nested = io.BytesIO()
        with ZipFile(nested, 'w') as z: z.writestr('usr/lib/' + dependency, elf(4096))
        apk = io.BytesIO()
        with ZipFile(apk, 'w') as z:
            z.writestr('lib/arm64-v8a/libnative.so', elf(16384))
            z.writestr('lib/arm64-v8a/libffmpeg.zip.so', nested.getvalue())
            if replacement: z.writestr('lib/arm64-v8a/' + dependency, elf(replacement))
        apk.seek(0)
        return apk

    def test_archive_dependency_cannot_bypass_alignment_check(self):
        with self.assertRaises(ValueError): native.check_apk(self.archive())

    def test_webp_replacement_must_itself_be_aligned(self):
        with self.assertRaises(ValueError): native.check_apk(self.archive(4096))
        self.assertEqual((2, 1, 1), native.check_apk(self.archive(16384)))

    def test_unexpected_old_dependency_is_not_whitelisted(self):
        with self.assertRaises(ValueError): native.check_apk(self.archive(16384, 'libsomething.so'))
