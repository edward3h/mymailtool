#!/bin/bash

abspath="$(cd "${0%/*}" 2>/dev/null; echo "$PWD"/"${0##*/}")"
myroot=$(dirname $abspath)
libdir=$myroot/dist/lib
outdir=$myroot/out
prddir=$outdir/production/Mailtool
tstdir=$outdir/test/Mailtool
prdsrc=$myroot/src
tsrsrc=$myroot/test

mkdir -p $prddir
mkdir -p $tstdir

# 1. compile classes
find $prdsrc -name '*.java' | xargs javac -sourcepath $prdsrc -classpath "$libdir/*" -d $prddir

# 2. compile test classes
find $tstsrc -name '*.java' | xargs javac -sourcepath "$tstsrc" -classpath "$prddir:$libdir/*:$libdir/jmock-2.5.1/*" -d $tstdir

# 3. run tests
ack -l --java '\@Test' test/ | cut -d '/' -f 2- | sed 's/\.java//' | sed 's/\//./g' | xargs java -classpath "$tstdir:$prddir:$libdir/*:$libdir/jmock-2.5.1/*" org.junit.runner.JUnitCore

# 4. build jar
jar cmfe manifest.mf $myroot/dist/Mailtool.jar org.ethelred.mymailtool2.Main -C $prddir org
