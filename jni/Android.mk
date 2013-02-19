LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
    jni_init.cpp \
    com_android_terminal_Terminal.cpp

LOCAL_C_INCLUDES += \
    external/libvterm/include \
    libcore/include

LOCAL_SHARED_LIBRARIES := \
    libutils \
    libnativehelper

LOCAL_STATIC_LIBRARIES := \
    libvterm

LOCAL_MODULE := libjni_terminal
LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)
