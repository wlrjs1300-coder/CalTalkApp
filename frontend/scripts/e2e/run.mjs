import { spawn, spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import process from 'node:process';
import { URL } from 'node:url';

const frontendDirectory = fileURLToPath(new URL('../../', import.meta.url));
const repositoryDirectory = fileURLToPath(new URL('../../../', import.meta.url));
const composeArguments = [
    'compose',
    '--env-file',
    'infra/.env.example',
    '-f',
    'infra/compose.yaml',
];
const services = ['postgres', 'redis'];
const serviceEnvironment = {
    ...process.env,
    POSTGRES_PORT: process.env.E2E_POSTGRES_PORT ?? '55432',
    REDIS_PORT: process.env.E2E_REDIS_PORT ?? '56379',
};

function docker(args, options = {}) {
    return spawnSync('docker', [...composeArguments, ...args], {
        cwd: repositoryDirectory,
        encoding: 'utf8',
        stdio: options.capture ? 'pipe' : 'inherit',
        env: serviceEnvironment,
    });
}

const version = spawnSync('docker', ['compose', 'version'], {
    cwd: repositoryDirectory,
    encoding: 'utf8',
    stdio: 'pipe',
});
if (version.status !== 0) {
    process.stderr.write('Docker Compose is required to run the browser E2E suite.\n');
    process.exit(version.status ?? 1);
}

const runningBeforeResult = docker(['ps', '--status', 'running', '--services'], { capture: true });
if (runningBeforeResult.status !== 0) process.exit(runningBeforeResult.status ?? 1);
const runningBefore = new Set(
    runningBeforeResult.stdout
        .split(/\r?\n/u)
        .map((line) => line.trim())
        .filter(Boolean),
);
const startedByRun = services.filter((service) => !runningBefore.has(service));

let exitCode;
try {
    const up = docker(['up', '-d', '--wait', ...services]);
    if (up.status !== 0) throw new Error('The E2E database services did not become ready.');

    const playwrightCli = fileURLToPath(
        new URL('../../node_modules/@playwright/test/cli.js', import.meta.url),
    );
    const playwright = spawn(process.execPath, [playwrightCli, 'test', ...process.argv.slice(2)], {
        cwd: frontendDirectory,
        stdio: 'inherit',
        env: {
            ...process.env,
            DB_URL: `jdbc:postgresql://localhost:${serviceEnvironment.POSTGRES_PORT}/caltalk`,
            DB_USERNAME: 'caltalk',
            DB_PASSWORD: 'caltalk-local-password',
        },
    });
    exitCode = await new Promise((resolve, reject) => {
        playwright.once('error', reject);
        playwright.once('exit', (code) => resolve(code ?? 1));
    });
} catch (error) {
    process.stderr.write(`${error instanceof Error ? error.message : String(error)}\n`);
} finally {
    if (startedByRun.length > 0) docker(['stop', ...startedByRun]);
}

process.exitCode = exitCode;
