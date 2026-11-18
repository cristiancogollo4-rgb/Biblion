#if canImport(SwiftUI)
import SwiftUI
import UIKit

enum BiblionTheme {
    static let blue = adaptive(light: 0x163E70, dark: 0xA8CBFF)
    static let gold = adaptive(light: 0x866216, dark: 0xE8C66C)
    static let paper = adaptive(light: 0xFAF9F6, dark: 0x101B2B)
    static let surface = adaptive(light: 0xFFFFFF, dark: 0x19283B)
    static let ink = adaptive(light: 0x152B43, dark: 0xF3F5F9)
    static let secondary = adaptive(light: 0x526176, dark: 0xB6C3D5)
    static let highlight = adaptive(light: 0xFFF0B3, dark: 0x55461F)
    static let divider = adaptive(light: 0xD7DEE6, dark: 0x3D5066)
    static let hero = Color(red: 0.07, green: 0.18, blue: 0.33)
    private static func adaptive(light: UInt32, dark: UInt32) -> Color {
        Color(uiColor: UIColor { traits in
            let value = traits.userInterfaceStyle == .dark ? dark : light
            return UIColor(red: CGFloat((value >> 16) & 255) / 255,
                           green: CGFloat((value >> 8) & 255) / 255,
                           blue: CGFloat(value & 255) / 255, alpha: 1)
        })
    }
}

struct StatusView: View {
    let title: String
    let message: String
    let symbol: String
    var actionTitle: String? = nil
    var action: (() -> Void)? = nil
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: symbol).font(.largeTitle).foregroundStyle(BiblionTheme.gold).accessibilityHidden(true)
            Text(title).font(.title2.bold()).multilineTextAlignment(.center)
            Text(message).font(.body).foregroundStyle(BiblionTheme.secondary).multilineTextAlignment(.center)
            if let actionTitle, let action {
                Button(actionTitle, action: action).buttonStyle(.borderedProminent).controlSize(.large)
            }
        }
        .padding(24).frame(maxWidth: 520).frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct VersionButton: View {
    let title: String
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            HStack(spacing: 5) {
                Text(title).font(.subheadline.weight(.semibold))
                Image(systemName: "chevron.down").font(.caption.weight(.bold))
            }.frame(minHeight: 44)
        }.accessibilityLabel("Cambiar versión bíblica. Actual: \(title)")
    }
}

extension View {
    func biblionPage() -> some View {
        self.foregroundStyle(BiblionTheme.ink)
            .background(BiblionTheme.paper.ignoresSafeArea())
            .toolbarBackground(BiblionTheme.paper, for: .navigationBar)
    }
}
#endif
