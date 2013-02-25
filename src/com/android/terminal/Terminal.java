/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.terminal;

import android.graphics.Color;

/**
 * Single terminal session backed by a pseudo terminal on the local device.
 */
public class Terminal {
    private static final String TAG = "Terminal";

    private static int sNumber = 0;

    static {
        System.loadLibrary("jni_terminal");
    }

    /**
     * Represents a run of one or more {@code VTermScreenCell} which all have
     * the same formatting.
     */
    public static class CellRun {
        char[] data;
        int dataSize;
        int colSize;

        boolean bold;
        int underline;
        boolean blink;
        boolean reverse;
        boolean strike;
        int font;

        int fg = Color.RED;
        int bg = Color.BLUE;
    }

    public interface TerminalClient {
        public void damage(int startRow, int endRow, int startCol, int endCol);
        public void moveRect(int destStartRow, int destEndRow, int destStartCol, int destEndCol,
                int srcStartRow, int srcEndRow, int srcStartCol, int srcEndCol);
        public void bell();
    }

    private final int mNativePtr;
    private final Thread mThread;

    private String mTitle;

    private TerminalClient mClient;

    private final TerminalCallbacks mCallbacks = new TerminalCallbacks() {
        @Override
        public int damage(int startRow, int endRow, int startCol, int endCol) {
            if (mClient != null) {
                mClient.damage(startRow, endRow, startCol, endCol);
            }
            return 1;
        }

        @Override
        public int moveRect(int destStartRow, int destEndRow, int destStartCol, int destEndCol,
                int srcStartRow, int srcEndRow, int srcStartCol, int srcEndCol) {
            if (mClient != null) {
                mClient.moveRect(destStartRow, destEndRow, destStartCol, destEndCol, srcStartRow,
                        srcEndRow, srcStartCol, srcEndCol);
            }
            return 1;
        }

        @Override
        public int bell() {
            if (mClient != null) {
                mClient.bell();
            }
            return 1;
        }
    };

    public Terminal() {
        mNativePtr = nativeInit(mCallbacks, 25, 80);
        mTitle = TAG + " " + sNumber++;
        mThread = new Thread(TAG) {
            @Override
            public void run() {
                nativeRun(mNativePtr);
            }
        };
    }

    /**
     * Start thread which internally forks and manages the pseudo terminal.
     */
    public void start() {
        mThread.start();
    }

    public void stop() {
        if (nativeStop(mNativePtr) != 0) {
            throw new IllegalStateException("stop failed");
        }
    }

    public void setClient(TerminalClient client) {
        mClient = client;
    }

    public void flushDamage() {
        if (nativeFlushDamage(mNativePtr) != 0) {
            throw new IllegalStateException("flushDamage failed");
        }
    }

    public void resize(int rows, int cols) {
        if (nativeResize(mNativePtr, rows, cols) != 0) {
            throw new IllegalStateException("resize failed");
        }
    }

    public int getRows() {
        return nativeGetRows(mNativePtr);
    }

    public int getCols() {
        return nativeGetCols(mNativePtr);
    }

    public void getCellRun(int row, int col, CellRun run) {
        if (nativeGetCellRun(mNativePtr, row, col, run) != 0) {
            throw new IllegalStateException("getCell failed");
        }
    }

    public String getTitle() {
        // TODO: hook up to title passed through termprop
        return mTitle;
    }

    public boolean dispatchKey(int modifiers, int key) {
        return nativeDispatchKey(mNativePtr, modifiers, key) == 0;
    }

    public boolean dispatchCharacter(int modifiers, int character) {
        return nativeDispatchCharacter(mNativePtr, modifiers, character) == 0;
    }


    private static native int nativeInit(TerminalCallbacks callbacks, int rows, int cols);
    private static native int nativeRun(int ptr);
    private static native int nativeStop(int ptr);

    private static native int nativeFlushDamage(int ptr);
    private static native int nativeResize(int ptr, int rows, int cols);
    private static native int nativeGetCellRun(int ptr, int row, int col, CellRun run);
    private static native int nativeGetRows(int ptr);
    private static native int nativeGetCols(int ptr);

    private static native int nativeDispatchKey(int ptr, int modifiers, int key);
    private static native int nativeDispatchCharacter(int ptr, int modifiers, int character);
}
