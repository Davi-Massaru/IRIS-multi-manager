Class MultiManager.Demo.Cleanup Extends %SYS.Task.Definition
{
Parameter TaskName = "DemoCleanup";
Method OnTask() As %Status
{
    Set ^MultiManagerDemo("cleanupLastRun")=$ZDATETIME($HOROLOG,3)
    Quit 1
}
}
