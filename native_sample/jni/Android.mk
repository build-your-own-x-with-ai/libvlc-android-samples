LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE    := native

LOCAL_SRC_FILES := main.c

LOCAL_LDLIBS := ${LIBVLC_LDLIBS} -llog

LOCAL_C_INCLUDES := $(VLC_SRC_DIR)/include

include $(BUILD_SHARED_LIBRARY)
