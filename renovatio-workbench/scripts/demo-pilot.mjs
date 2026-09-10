import { createHash } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import process from 'node:process';

const root = new URL('..', import.meta.url);
const hardening = JSON.parse(await readFile(new URL('config/release-hardening.json', root), 'utf8'));
const failures = [];

for (const fixture of hardening.pilot.fixtures) {
    const bytes = await readFile(new URL(fixture.path, root));
    const actual = createHash('sha256').update(bytes).digest('hex');
    if (actual !== fixture.sha256) {
        failures.push(`${fixture.path}: expected ${fixture.sha256}, got ${actual}`);
    }
}

const cobolFixtures = hardening.pilot.fixtures.filter(fixture => fixture.path.endsWith('.cob'));
const irFixtures = hardening.pilot.fixtures.filter(fixture => fixture.path.endsWith('.json'));

if (cobolFixtures.length < 3) {
    failures.push('Pilot must include the three COBOL demo programs');
}
if (irFixtures.length < 3) {
    failures.push('Pilot must include the three demo IR files');
}

if (failures.length > 0) {
    console.error('Demo pilot validation failed:');
    for (const failure of failures) {
        console.error(`- ${failure}`);
    }
    process.exit(1);
}

console.log(`Demo pilot validated: ${hardening.pilot.name} (${cobolFixtures.length} COBOL programs, ${irFixtures.length} IR files, ${hardening.pilot.fixtures.length} total fixtures).`);
