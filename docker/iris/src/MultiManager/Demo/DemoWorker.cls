Class MultiManager.Demo.DemoWorker
{
ClassMethod Run()
{
    Lock +^MultiManagerDemo("worker"):0
    Quit:'$TEST
    Set ^MultiManagerDemo("workerPid")=$JOB
    For { Hang 5 }
}
}
