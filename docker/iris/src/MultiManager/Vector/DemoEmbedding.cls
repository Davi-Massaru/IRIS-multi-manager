Class MultiManager.Vector.DemoEmbedding Extends %Embedding.Interface
{

ClassMethod IsValidConfig(config As %DynamicObject, errorMsg As %String) As %Boolean [ Language = python ]
{
    if hasattr(errorMsg, "value"):
        errorMsg.value = ""
    return True
}

/// A deterministic, dependency-free Embedded Python provider used only by the
/// local demo. Real deployments can register their own %Embedding.Interface.
ClassMethod Embedding(input, configuration) As %Vector [ Language = python ]
{
    import hashlib
    import json

    digest = hashlib.sha256(str(input).encode("utf-8")).digest()
    values = [((digest[i] / 255.0) * 2.0) - 1.0 for i in range(4)]
    return json.dumps(values)
}

ClassMethod EmbeddingJSON(input As %String) As %String [ Language = python, Private ]
{
    import hashlib
    import json

    digest = hashlib.sha256(str(input).encode("utf-8")).digest()
    values = [round(((digest[i] / 255.0) * 2.0) - 1.0, 8) for i in range(4)]
    return json.dumps(values)
}

}
