Class MultiManager.Demo.Seed
{
ClassMethod Run() As %Status [ Language = python ]
{
    import iris
    import os

    profile = os.environ.get("DEMO_PROFILE")
    if profile not in {"PROD1", "PROD2", "PROD3"}:
        raise ValueError("Invalid demo profile")

    iris.execute('zn "%SYS"')
    applications = iris.cls("Security.Applications")
    for name in ("/api/demo", "/api/internal"):
        if name == "/api/internal" and profile == "PROD3":
            continue
        if not applications.Exists(name):
            properties = iris.arrayref({
                "NameSpace": "USER",
                "Enabled": 1,
                "AutheEnabled": 32,
                "Description": "Multi-Manager demo fixture"
            })
            iris.check_status(applications.Create(name, properties))

    iris.execute('zn "USER"')
    iris.check_status(iris.cls("MultiManager.Demo.Seed").SeedVector(profile))
    demo = iris.gref("^MultiManagerDemo")
    if demo.data(["seeded"]) == 0:
        counts = {"PROD1": 3, "PROD2": 5, "PROD3": 7}
        person_class = iris.cls("MultiManager.Demo.Person")
        for number in range(1, counts[profile] + 1):
            person = person_class._New()
            person.Name = f"Person {number}"
            person.InstanceName = profile
            iris.check_status(person._Save())

        task_classes = ["MultiManager.Demo.Cleanup"]
        if profile == "PROD2":
            task_classes.append("MultiManager.Demo.Audit")
        task_class = iris.cls("%SYS.Task")
        for class_name in task_classes:
            task = task_class._New()
            task.Name = "DemoCleanup" if class_name.endswith("Cleanup") else "DemoAudit"
            task.TaskClass = class_name
            task.NameSpace = "USER"
            task.TimePeriod = 0
            iris.check_status(task._Save())
        demo["seeded"] = 1

    iris.cls("MultiManager.Demo.DemoWorker").Run()
    return 1
}

ClassMethod SeedVector(profile As %String) As %Status [ Language = python ]
{
    import iris
    import json

    compiled_class = iris.cls("%Dictionary.CompiledClass")
    compiled_index = iris.cls("%Dictionary.CompiledIndex")

    if profile == "PROD3":
        if not compiled_class._ExistsId("MultiManager.Vector.Document"):
            iris.sql.exec("CREATE TABLE MultiManager_Vector.Document (ID INTEGER IDENTITY PRIMARY KEY, Title VARCHAR(100), Content VARCHAR(1000), VectorData VECTOR(DOUBLE,4))")
        total = next(iter(iris.sql.exec("SELECT COUNT(*) FROM MultiManager_Vector.Document")))[0]
        if total == 0:
            insert = iris.sql.prepare("INSERT INTO MultiManager_Vector.Document (Title,Content,VectorData) VALUES (?,?,TO_VECTOR(?,DOUBLE))")
            for number in range(1, 6):
                vector = json.dumps([number / 10, (number + 1) / 10, (number + 2) / 10, (number + 3) / 10])
                insert.execute(f"Vector document {number}", f"{profile} source text {number}", vector)
    else:
        config = json.dumps({
            "modelName": "multimanager/deterministic-demo",
            "sentenceTransformersCandidate": "sentence-transformers/all-MiniLM-L6-v2",
            "hfCachePath": "/durable/models",
            "apiKey": "demo-secret-never-return"
        }, separators=(",", ":"))
        total = next(iter(iris.sql.exec("SELECT COUNT(*) FROM %Embedding.Config WHERE Name='multimanager-demo'")))[0]
        if total == 0:
            insert_config = iris.sql.prepare("INSERT INTO %Embedding.Config (Name,Configuration,EmbeddingClass,VectorLength,Description) VALUES (?,?,?,?,?)")
            insert_config.execute("multimanager-demo", config, "MultiManager.Vector.DemoEmbedding", 4, "Dependency-free Embedded Python demo provider")
        if not compiled_class._ExistsId("MultiManager.Vector.EmbeddingDocument"):
            iris.sql.exec("CREATE TABLE MultiManager_Vector.EmbeddingDocument (ID INTEGER IDENTITY PRIMARY KEY, Title VARCHAR(100), Content VARCHAR(1000), EmbeddingData EMBEDDING('multimanager-demo','Content'))")
        total = next(iter(iris.sql.exec("SELECT COUNT(*) FROM MultiManager_Vector.EmbeddingDocument")))[0]
        if total == 0:
            insert_document = iris.sql.prepare("INSERT INTO MultiManager_Vector.EmbeddingDocument (Title,Content,EmbeddingData) VALUES (?,?,TO_VECTOR(?,DOUBLE))")
            limit = 3 if profile == "PROD1" else 4
            for number in range(1, limit + 1):
                content = f"{profile} managed embedding {number}"
                vector = iris.cls("MultiManager.Vector.DemoEmbedding").EmbeddingJSON(content)
                insert_document.execute(f"Embedded document {number}", content, vector)
        if not compiled_index._ExistsId("MultiManager.Vector.EmbeddingDocument||EmbeddingHNSW"):
            iris.sql.exec("CREATE INDEX EmbeddingHNSW ON TABLE MultiManager_Vector.EmbeddingDocument (EmbeddingData) AS HNSW(Distance='Cosine')")
    return 1
}
}
