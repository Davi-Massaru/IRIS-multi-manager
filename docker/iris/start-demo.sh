#!/bin/sh
set -eu
iris session IRIS -U USER <<'OBJECTSCRIPT'
set $ETRAP="write $ZERROR,! zhalt 1"
set sc=$SYSTEM.OBJ.LoadDir("/opt/multi-manager/src","ck",,1)
if $SYSTEM.Status.IsError(sc) { do $SYSTEM.Status.DisplayError(sc) zhalt 1 }
set sc=##class(MultiManager.Demo.Seed).Run()
if $SYSTEM.Status.IsError(sc) { do $SYSTEM.Status.DisplayError(sc) zhalt 1 }
halt
OBJECTSCRIPT
