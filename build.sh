#!/bin/bash

abspath="$(cd "${0%/*}" 2>/dev/null; echo "$PWD"/"${0##*/}")"
myroot=$(dirname $abspath)

. $myroot/set_ack.sh

if [[ -z $ACK_CMD ]]; then
    echo "Can't find ack-grep or ack command - please set ACK_CMD"
    exit 1
fi

libdir=$myroot/dist/lib
outdir=$myroot/out
prddir=$outdir/production/Mailtool
tstdir=$outdir/test/Mailtool
prdsrc=$myroot/src
tstsrc=$myroot/test

mkdir -p $prddir
mkdir -p $tstdir

# 1. compile classes
find $prdsrc -name '*.java' | xargs javac -sourcepath $prdsrc -classpath "$libdir/*" -d $prddir

# 2. compile test classes
find $tstsrc -name '*.java' | xargs javac -sourcepath "$tstsrc" -classpath "$prddir:$libdir/*:$libdir/jmock-2.5.1/*" -d $tstdir

# 2.5. copy test resources
for f in $(find $tstsrc -name '*.js' -or -name '*.properties' -or -name 'javamail.providers' -printf '%P\n')
do
RDIR="${tstdir}/$(dirname $f)"
echo $RDIR
mkdir -p $RDIR
cp $tstsrc/$f $RDIR
done

# 3. run tests
$ACK_CMD -l --java '\@Test' test/ | cut -d '/' -f 2- | sed 's/\.java//' | sed 's/\//./g' | xargs java -classpath "$tstdir:$prddir:$libdir/*:$libdir/jmock-2.5.1/*" org.junit.runner.JUnitCore

# 4. build jar
jar cmfe manifest.mf $myroot/dist/Mailtool.jar org.ethelred.mymailtool2.Main -C $prddir org
