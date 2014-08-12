LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_CFLAGS		:= -Werror 
LOCAL_MODULE		:= wdt_tdi 
LOCAL_SRC_FILES		:= wdt_tdi.cpp wdt_tdi_login.cpp wdt_tdi_query.cpp wdt_tdi_update.cpp

LOCAL_SRC_FILES		+= tdi_prot.c 
LOCAL_SRC_FILES		+= rc4.c md5.c 
LOCAL_SRC_FILES		+= miniz.c sshbn.c 

LOCAL_CFLAGS		= -std=c99
LOCAL_LDLIBS		:= -llog 
include $(BUILD_SHARED_LIBRARY)
