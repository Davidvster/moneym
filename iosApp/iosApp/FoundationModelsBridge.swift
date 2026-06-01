import Foundation
import ComposeApp
#if canImport(FoundationModels)
import FoundationModels
#endif

final class FoundationModelsBridgeImpl: NSObject, IosAiBridge {

    func isAvailable() -> Bool {
        #if canImport(FoundationModels)
        if #available(iOS 26.0, *) {
            if case .available = SystemLanguageModel.default.availability { return true }
        }
        #endif
        return false
    }

    func streamReply(
        prompt: String,
        onChunk: @escaping (String) -> Void,
        onComplete: @escaping () -> Void,
        onError: @escaping (String) -> Void
    ) {
        #if canImport(FoundationModels)
        if #available(iOS 26.0, *) {
            Task {
                do {
                    let session = LanguageModelSession()
                    let stream = session.streamResponse(to: prompt)
                    for try await partial in stream {
                        onChunk(partial.content)
                    }
                    onComplete()
                } catch {
                    onError(error.localizedDescription)
                }
            }
            return
        }
        #endif
        onError("Foundation Models unavailable")
    }
}
