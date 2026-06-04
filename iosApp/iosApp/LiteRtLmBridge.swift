import Foundation
import ComposeApp
#if canImport(LiteRTLM)
import LiteRTLM
#endif

/// Swift side of the on-device LLM runtime for iOS, backed by LiteRT-LM.
///
/// Until the LiteRT-LM Swift package is added to the Xcode project
/// (see plan/ai-analysis-on-device/IOS_MANUAL_STEPS.md), `canImport(LiteRTLM)`
/// is false and this bridge reports the engine as unavailable, so the on-device
/// option shows as unavailable on iOS while the app builds and runs normally.
/// Once the package is added, the `#if canImport` branch activates with no
/// further Kotlin or Swift changes.
final class LiteRtLmBridgeImpl: NSObject, IosLocalLlmBridge {

    #if canImport(LiteRTLM)
    private var engine: Engine?
    private var loadedPath: String?
    #endif

    func isLoaded() -> Bool {
        #if canImport(LiteRTLM)
        return engine != nil
        #else
        return false
        #endif
    }

    func loadModel(path: String, onResult: @escaping (KotlinBoolean) -> Void) {
        #if canImport(LiteRTLM)
        if loadedPath == path, engine != nil {
            onResult(KotlinBoolean(bool: true))
            return
        }
        Task {
            do {
                let config = try EngineConfig(
                    modelPath: path,
                    backend: .gpu,
                    cacheDir: NSTemporaryDirectory()
                )
                let engine = Engine(engineConfig: config)
                try await engine.initialize()
                self.engine = engine
                self.loadedPath = path
                onResult(KotlinBoolean(bool: true))
            } catch {
                self.engine = nil
                self.loadedPath = nil
                onResult(KotlinBoolean(bool: false))
            }
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
        #if canImport(LiteRTLM)
        guard let engine = engine else {
            onError("On-device model not loaded")
            return
        }
        Task {
            do {
                let conversation = try await engine.createConversation()
                // LiteRT-LM streams incremental chunks; the Kotlin runner expects
                // a cumulative string, so accumulate before forwarding.
                var cumulative = ""
                for try await chunk in conversation.sendMessageStream(Message(prompt)) {
                    cumulative += chunk.toString
                    onChunk(cumulative)
                }
                onComplete()
            } catch {
                onError(error.localizedDescription)
            }
        }
        #else
        onError("On-device model runtime not linked")
        #endif
    }
}
