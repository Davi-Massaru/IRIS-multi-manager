Class MultiManager.Demo.Seed
{
ClassMethod Run() As %Status
{
    Set sc=1
    Try {
        Set profile=$SYSTEM.Util.GetEnviron("DEMO_PROFILE")
        If (profile'="PROD1")&&(profile'="PROD2")&&(profile'="PROD3") {
            Throw ##class(%Exception.General).%New("Invalid demo profile")
        }
        Set $NAMESPACE="%SYS"
        For name="/api/demo","/api/internal" {
            Continue:(name="/api/internal")&&(profile="PROD3")
            If '##class(Security.Applications).Exists(name) {
                Kill props
                Set props("NameSpace")="USER",props("Enabled")=1
                Set props("AutheEnabled")=32,props("Description")="Multi-Manager demo fixture"
                Set sc=##class(Security.Applications).Create(name,.props)
                Throw:$$$ISERR(sc) ##class(%Exception.StatusException).CreateFromStatus(sc)
            }
        }
        Set $NAMESPACE="USER"
        If '$Data(^MultiManagerDemo("seeded")) {
            For i=1:1:$Select(profile="PROD1":3,profile="PROD2":5,1:7) {
                Set person=##class(MultiManager.Demo.Person).%New()
                Set person.Name="Person "_i,person.InstanceName=profile
                Set sc=person.%Save()
                Throw:$$$ISERR(sc) ##class(%Exception.StatusException).CreateFromStatus(sc)
            }
            For taskClass="MultiManager.Demo.Cleanup","MultiManager.Demo.Audit" {
                Continue:(taskClass="MultiManager.Demo.Audit")&&(profile'="PROD2")
                Set task=##class(%SYS.Task).%New()
                Set task.Name=$Select(taskClass="MultiManager.Demo.Cleanup":"DemoCleanup",1:"DemoAudit")
                Set task.TaskClass=taskClass,task.NameSpace="USER",task.TimePeriod=0
                Set sc=task.%Save()
                Throw:$$$ISERR(sc) ##class(%Exception.StatusException).CreateFromStatus(sc)
            }
            Set ^MultiManagerDemo("seeded")=1
        }
        Job ##class(MultiManager.Demo.DemoWorker).Run()
    } Catch error {
        Set sc=error.AsStatus()
    }
    Quit sc
}
}
