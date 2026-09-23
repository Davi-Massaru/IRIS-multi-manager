Class MultiManager.Vector.Extension
{

/// Reports the real Embedded Python and SentenceTransformers state without
/// importing or downloading a model as a side effect.
ClassMethod Capabilities() As %String [ Language = python, SqlName = MM_VectorCapabilities, SqlProc ]
{
    import importlib.util
    import json
    import os
    import platform

    available = importlib.util.find_spec("sentence_transformers") is not None
    return json.dumps({
        "extensionVersion": "1.0.0",
        "embeddedPython": True,
        "pythonVersion": platform.python_version(),
        "sentenceTransformersInstalled": available,
        "profile": os.environ.get("DEMO_PROFILE", "UNKNOWN")
    })
}

ClassMethod CheckModel(modelName As %String = "sentence-transformers/all-MiniLM-L6-v2") As %String [ Language = python, SqlName = MM_VectorCheckModel, SqlProc ]
{
    import importlib.util
    import json

    installed = importlib.util.find_spec("sentence_transformers") is not None
    return json.dumps({
        "model": modelName,
        "runtimeInstalled": installed,
        "ready": False,
        "message": "Runtime is available; model loading is only performed by an explicit test or download action." if installed else "sentence_transformers is not installed."
    })
}

/// The demo deliberately does not perform an implicit network download.
ClassMethod DownloadModel(modelName As %String) As %String [ Language = python, SqlName = MM_VectorDownloadModel, SqlProc ]
{
    import json
    return json.dumps({
        "model": modelName,
        "downloaded": False,
        "message": "Automatic model downloads are disabled in this offline demo image."
    })
}

/// Generates a small deterministic vector in-process. It is suitable for the
/// demo recipe and proves the SqlProc + Embedded Python execution path.
ClassMethod TestModel(text As %String) As %String [ Language = python, SqlName = MM_VectorTestModel, SqlProc ]
{
    import hashlib
    import json

    digest = hashlib.sha256(str(text).encode("utf-8")).digest()
    values = [round(((digest[i] / 255.0) * 2.0) - 1.0, 8) for i in range(4)]
    return json.dumps({"dimensions": 4, "vector": values, "provider": "MultiManager deterministic demo recipe"})
}

ClassMethod Regenerate(text As %String) As %String [ Language = python, SqlName = MM_VectorRegenerate, SqlProc ]
{
    import hashlib
    import json

    digest = hashlib.sha256(str(text).encode("utf-8")).digest()
    values = [round(((digest[i] / 255.0) * 2.0) - 1.0, 8) for i in range(4)]
    return json.dumps(values)
}

}
