import Foundation
import ComposeApp
#if canImport(MediaPipeTasksGenAI)
import MediaPipeTasksGenAI
#endif

/// Swift side of the on-device LLM runtime for iOS.
///
/// Until the MediaPipe / LiteRT-LM framework is linked into the Xcode project
/// (see plan/ai-analysis-on-device/IOS_MANUAL_STEPS.md), `canImport` is false and
/// this bridge reports "not linked" so the on-device engine shows as unavailable.
/// Once the pod/xcframework is added, the `#if canImport` branch activates with no
/// further Kotlin changes.
final class MediaPipeBridgeImpl: NSObject, IosLocalLlmBridge {

    #if canImport(MediaPipeTasksGenAI)
    private var inference: LlmInference?
    private var loadedPath: String?
    #endif

    func isLoaded() -> Bool {
        #if canImport(MediaPipeTasksGenAI)
        return inference != nil
        #else
        return false
        #endif
    }

    func loadModel(path: String, onResult: @escaping (KotlinBoolean) -> Void) {
        #if canImport(MediaPipeTasksGenAI)
        if loadedPath == path, inference != nil {
            onResult(KotlinBoolean(bool: true))
            return
        }
        do {
            let options = LlmInference.Options(modelPath: path)
            inference = try LlmInference(options: options)
            loadedPath = path
            onResult(KotlinBoolean(bool: true))
        } catch {
            inference = nil
            loadedPath = nil
            onResult(KotlinBoolean(bool: false))
        }
        #else
        onResult(KotlinBoolean(bool: false))
        #endif
    }

    func streamReply(
        prompt: String,
        onChunk: @escaping (String) -> Void,
        onComplete: @escaping () -> Void,
        onError: @escaping (String) -> Void
    ) {
        #if canImport(MediaPipeTasksGenAI)
        guard let inference = inference else {
            onError("On-device model not loaded")
            return
        }
        do {
            // MediaPipe delivers incremental tokens; the Kotlin runner expects a
            // cumulative string, so accumulate before forwarding.
            var cumulative = ""
            try inference.generateResponseAsync(
                inputText: prompt,
                progress: { partial, error in
                    if let error = error {
                        onError(error.localizedDescription)
                        return
                    }
                    if let partial = partial {
                        cumulative += partial
                        onChunk(cumulative)
                    }
                },
                completion: {
                    onComplete()
                }
            )
        } catch {
            onError(error.localizedDescription)
        }
        #else
        onError("On-device model runtime not linked")
        #endif
    }
}
