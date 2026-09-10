# Package shim to expose specs/1-cobol-python-migration as a proper importable package
import os
_repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__)))
# Determine the path to the spec dir
_spec_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), 'specs', '1-cobol-python-migration'))
# If the spec dir exists at that location, insert it into package __path__ so subpackages like 'tools' become importable
if os.path.isdir(_spec_dir):
    __path__.insert(0, _spec_dir)
else:
    # Fallback: look for ../specs/1-cobol-python-migration
    alt = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'specs', '1-cobol-python-migration'))
    if os.path.isdir(alt):
        __path__.insert(0, alt)

