import { app, BrowserWindow } from 'electron';
import path from 'path';
import { spawn } from 'child_process';
import { fileURLToPath } from 'url';
import { dirname } from 'path';
import http from 'http';
import fs from 'fs';
import net from 'net';
import { dialog } from 'electron';

// Electron flags (optional)
app.commandLine.appendSwitch('force-device-scale-factor', '1');
app.commandLine.appendSwitch('disable-gpu');
app.commandLine.appendSwitch('no-sandbox');

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

let mainWindow;
let backend;

// Check if a port is already in use
function isPortInUse(port) {
    return new Promise((resolve) => {
        const server = net.createServer();

        server.once('error', err => {
            if (err.code === 'EADDRINUSE') {
                resolve(true);
            } else {
                resolve(false);
            }
        });

        server.once('listening', () => {
            server.close();
            resolve(false);
        });

        server.listen(port);
    });
}

// Helper to write to log
function writeToLog(filename, message) {
    const logDir = path.join(app.getPath('userData'), 'logs');
    if (!fs.existsSync(logDir)) {
        fs.mkdirSync(logDir, { recursive: true });
    }
    const logPath = path.join(logDir, filename);
    fs.appendFileSync(logPath, `[${new Date().toISOString()}] ${message}\n`);
}

// Global Electron error logging
process.on('uncaughtException', (err) => {
    console.error('❌ Uncaught Exception:', err);
    writeToLog('electron-error.log', err.stack || err.toString());
});

// Wait for backend to be ready
function waitForBackendReady(url, maxAttempts = 30, delay = 1000) {
    let attempts = 0;
    return new Promise((resolve, reject) => {
        const check = () => {
            attempts++;
            console.log(`Checking backend (${attempts}/${maxAttempts})...`);

            http.get(url, (res) => {
                if (res.statusCode === 200) {
                    console.log('✅ Backend is ready');
                    resolve();
                } else {
                    retry();
                }
            }).on('error', retry);
        };

        const retry = () => {
            if (attempts >= maxAttempts) {
                reject(new Error('❌ Backend did not start in time'));
            } else {
                setTimeout(check, delay);
            }
        };

        check();
    });
}


// Create the Electron Browser Window
function createWindow() {
    mainWindow = new BrowserWindow({
        width: 800,
        height: 600,
        webPreferences: {
            contextIsolation: true,
            nodeIntegration: false
        }
    });

    if (process.env.NODE_ENV === 'development') {
        mainWindow.webContents.openDevTools();
    }

    mainWindow.loadFile(path.join(__dirname, 'react-ui', 'dist', 'index.html'))
        .catch((err) => {
            console.error('❌ Error loading HTML file:', err);
        });

    mainWindow.webContents.on('did-fail-load', (errorCode, errorDescription, validatedURL) => {
        console.error(`❌ Failed to load page: ${validatedURL} - ${errorDescription} (Error code: ${errorCode})`);
    });
}

// JAR and Java paths
const jarPath = path.join(process.resourcesPath, 'app.asar.unpacked', 'java-backend', 'backend-0.0.1-SNAPSHOT.jar');
const javaPath = path.join(process.resourcesPath, 'app.asar.unpacked', 'java-runtime', 'bin', 'javaw.exe');


// Electron single instance lock (optional but safer)
const gotTheLock = app.requestSingleInstanceLock();

if (!gotTheLock) {
    app.quit();
} else {
    app.whenReady().then(async () => {
        const is8080Used = await isPortInUse(8080);

        if (is8080Used) {
            const msg = '⚠️ Backend already running — likely from browser. Close browser or kill the process before starting Electron.';
            console.log(msg);
            writeToLog('backend.log', msg);

            dialog.showMessageBoxSync({
                type: 'warning',
                title: 'Port Conflict',
                message: 'The backend is already running (port 8080 in use).\n\nOnly one instance (browser or app) can be active.',
                buttons: ['OK']
            });

            return; // ❗ Prevent backend from launching
        }

        const msg = '🚀 Starting backend...';
        console.log(msg);
        writeToLog('backend.log', msg);

        if (!fs.existsSync(jarPath)) {
            const msg = `❌ JAR file does not exist at path: ${jarPath}`;
            console.error(msg);
            writeToLog('backend-error.log', msg);
        }

        if (!fs.existsSync(javaPath)) {
            const msg = `❌ Java executable does not exist at path: ${javaPath}`;
            console.error(msg);
            writeToLog('backend-error.log', msg);
        }

        backend = spawn(javaPath, ['-jar', jarPath], {
            windowsHide: true,
            detached: false,
            stdio: ['ignore', 'pipe', 'pipe']
        });




        backend.on('error', (err) => {
            console.error('❌ Failed to start backend:', err);
            writeToLog('backend-error.log', `Failed to start backend: ${err.message}`);
        });

        backend.on('exit', (code, signal) => {
            const msg = `Backend exited with code ${code} and signal ${signal}`;
            console.error(`❌ ${msg}`);
            writeToLog('backend-error.log', msg);
        });

        backend.stdout.on('data', (data) => {
            const msg = data.toString();
            console.log(`Backend stdout: ${msg}`);
            writeToLog('backend-out.log', msg);
        });

        backend.stderr.on('data', (data) => {
            const msg = data.toString();
            console.error(`Backend stderr: ${msg}`);
            writeToLog('backend-error.log', msg);
        });

        try {
            await waitForBackendReady('http://localhost:8080/api/machine-types');
            createWindow();
        } catch (err) {
            console.error('❌ Startup error:', err);
            writeToLog('backend-error.log', `Startup error: ${err.message}`);
        }

    });

    app.on('before-quit', () => {
        console.log('App is about to quit.');
        // Additional logic to handle cleanup or save state can go here
    });


}