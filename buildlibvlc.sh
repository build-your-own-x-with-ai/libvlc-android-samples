#!/bin/sh

rootdir=`dirname $0`

checkfail()
{
    if [ ! $? -eq 0 ];then
        echo "'$1' failed"
        exit 1
    fi
}

if [ ! -d "${rootdir}/libvlcjni" ]; then
    echo "VLC Android source not found, cloning"
    git clone http://code.videolan.org/videolan/libvlcjni.git
    checkfail "git clone"
fi

sh -c "cd ${rootdir}/libvlcjni && ./buildsystem/compile-libvlc.sh $*"
checkfail "./buildsystem/compile-libvlc.sh $*"

./gradlew -p libvlcjni/libvlc assembleDebug -PlibvlcVersion=4.0.0 -PforceVlc4=true
aar_file="${rootdir}/libvlcjni/libvlc/build/outputs/aar/libvlc-debug.aar"
cp "${aar_file}" "${rootdir}"/libvlc/libvlc-4.0.0.aar
checkfail "libvlc*.arr not found"

VLC_SRC_DIR=`realpath "${rootdir}"/libvlcjni/vlc`
LIBVLCJNI_LIBS=`realpath "${rootdir}"/libvlcjni/libvlc/jni/libs`

for project in native_sample;do
    for jnilinkdir in ${LIBVLCJNI_LIBS}/*;do
        arch=`basename $jnilinkdir`
        if [ ! -f "${jnilinkdir}/libvlcjni.so" ];then
            continue;
        fi
        $ANDROID_NDK/ndk-build -C "${rootdir}"/${project} \
            VLC_SRC_DIR="${VLC_SRC_DIR}" \
            LIBVLC_LDLIBS="-L${jnilinkdir} -lvlc -lvlcjni" \
            APP_BUILD_SCRIPT=jni/Android.mk \
            APP_PLATFORM=android-21 \
            APP_ABI=${arch} \
            NDK_PROJECT_PATH=jni
    done
done
