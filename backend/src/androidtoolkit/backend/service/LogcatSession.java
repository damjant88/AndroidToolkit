package androidtoolkit.backend.service;

import androidtoolkit.backend.dto.LogcatData;

/**
 * Internal state tracking for an active logcat stream session.
 * Each connected device with a valid PID has exactly one LogcatSession.
 */
class LogcatSession {

    private String serial;
    private String pid;
    private Process process;
    private Thread readerThread;
    private StreamState state;
    private int restartAttempts;
    private LogcatData currentData;

    LogcatSession(String serial, String pid) {
        this.serial = serial;
        this.pid = pid;
        this.state = StreamState.STOPPED;
        this.restartAttempts = 0;
        this.currentData = new LogcatData(serial);
    }

    String getSerial() {
        return serial;
    }

    void setSerial(String serial) {
        this.serial = serial;
    }

    String getPid() {
        return pid;
    }

    void setPid(String pid) {
        this.pid = pid;
    }

    Process getProcess() {
        return process;
    }

    void setProcess(Process process) {
        this.process = process;
    }

    Thread getReaderThread() {
        return readerThread;
    }

    void setReaderThread(Thread readerThread) {
        this.readerThread = readerThread;
    }

    StreamState getState() {
        return state;
    }

    void setState(StreamState state) {
        this.state = state;
    }

    int getRestartAttempts() {
        return restartAttempts;
    }

    void setRestartAttempts(int restartAttempts) {
        this.restartAttempts = restartAttempts;
    }

    LogcatData getCurrentData() {
        return currentData;
    }

    void setCurrentData(LogcatData currentData) {
        this.currentData = currentData;
    }
}
