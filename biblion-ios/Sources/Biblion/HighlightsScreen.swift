#if canImport(SwiftUI)
import SwiftUI
import BiblionCore

struct HighlightsScreen: View {
    @EnvironmentObject private var app: AppModel
    var body: some View {
        Group {
            if app.state.highlights.isEmpty {
                StatusView(title: "Palabras para volver a leer", message: "Mantén pulsado un versículo en el lector y elige Resaltar. Lo encontrarás aquí.", symbol: "bookmark")
            } else {
                List {
                    Section {
                        ForEach(app.state.highlights) { saved in
                            NavigationLink(value: AppRoute.reader(saved.location)) {
                                VStack(alignment: .leading, spacing: 10) {
                                    Text(saved.location.reference).font(.headline).foregroundStyle(BiblionTheme.blue)
                                    Text(saved.text).font(.body).lineLimit(4).foregroundStyle(BiblionTheme.ink)
                                    Text(app.version(saved.location.version)?.name ?? saved.location.version.uppercased())
                                        .font(.caption).foregroundStyle(BiblionTheme.secondary)
                                }.padding(.vertical, 12)
                            }
                            .swipeActions { Button("Quitar", role: .destructive) { app.toggleHighlight(saved.location, text: saved.text) } }
                            .listRowBackground(BiblionTheme.surface)
                        }
                    } footer: { Text("Tus resaltados se guardan en este dispositivo.") }
                }.listStyle(.insetGrouped).scrollContentBackground(.hidden)
            }
        }.navigationTitle("Resaltados").biblionPage()
    }
}
#endif
