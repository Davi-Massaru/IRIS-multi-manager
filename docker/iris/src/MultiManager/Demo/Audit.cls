Class MultiManager.Demo.Audit Extends %SYS.Task.Definition
{
Parameter TaskName = "DemoAudit";
Method OnTask() As %Status
{
    Set ^MultiManagerDemo("auditLastRun")=$ZDATETIME($HOROLOG,3)
    Quit 1
}
}
