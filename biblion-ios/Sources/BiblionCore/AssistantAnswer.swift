import Foundation

public enum AssistantAnswer {
    /// Only display prose or the explicit answer field; never leak a transport envelope.
    public static func clean(_ raw: String) -> String? {
        var text = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        for _ in 0..<4 {
            if text.hasPrefix("```json"), text.hasSuffix("```") {
                text = String(text.dropFirst(7).dropLast(3)).trimmingCharacters(in: .whitespacesAndNewlines)
            } else if text.hasPrefix("```"), text.hasSuffix("```"), text.dropFirst(3).trimmingCharacters(in: .whitespacesAndNewlines).hasPrefix("{") {
                text = String(text.dropFirst(3).dropLast(3)).trimmingCharacters(in: .whitespacesAndNewlines)
            }
            guard !text.isEmpty else { return nil }
            guard text.hasPrefix("{") else { return text }
            guard let object = try? JSONSerialization.jsonObject(with: Data(text.utf8)) as? [String: Any], let answer = object["answer"] as? String else { return nil }
            text = answer.trimmingCharacters(in: .whitespacesAndNewlines)
        }
        return text.hasPrefix("{") || text.isEmpty ? nil : text
    }
}
