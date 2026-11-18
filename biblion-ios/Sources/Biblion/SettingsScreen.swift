#if canImport(SwiftUI)
import SwiftUI
import BiblionCore

struct SettingsScreen: View {
    @EnvironmentObject private var app: AppModel
    @State private var showVersions = false
    var body: some View {
        Form {
            Section("Lectura") {
                Button { showVersions = true } label: {
                    HStack {
                        Text("Versión bíblica")
                        Spacer()
                        Text(app.version(app.state.selectedVersion)?.abbreviation ?? "")
                            .foregroundStyle(BiblionTheme.secondary)
                        Image(systemName: "chevron.right").accessibilityHidden(true)
                    }.frame(minHeight: 44)
                }
                FontSizeControl()
                Text("La Palabra acompaña tu día.")
                    .font(.custom("Georgia", size: app.state.fontSize, relativeTo: .body))
                    .padding(.vertical, 12).accessibilityLabel("Vista previa del tamaño de lectura. La Palabra acompaña tu día.")
            }
            Section("Apariencia") {
                Picker("Tema", selection: Binding(get: { app.state.appearance }, set: { value in app.update { $0.appearance = value } })) {
                    ForEach(AppAppearance.allCases, id: \.self) { Text($0.title).tag($0) }
                }
            }
            Section {
                Label("Lectura sin conexión", systemImage: "checkmark.circle")
                Text("La última lectura, tus resaltados y tus ajustes se guardan en este dispositivo.")
                    .foregroundStyle(BiblionTheme.secondary)
            } header: { Text("Tus datos") }
            Section("Acerca de Biblion") {
                LabeledContent("Versión", value: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "")
                Text("La Palabra, cada día.").foregroundStyle(BiblionTheme.secondary)
            }
        }
        .navigationTitle("Ajustes").scrollContentBackground(.hidden)
        .sheet(isPresented: $showVersions) { VersionPicker { app.selectVersion($0) } }
        .biblionPage()
    }
}

struct FontSizeControl: View {
    @EnvironmentObject private var app: AppModel
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack { Text("Tamaño de lectura"); Spacer(); Text("\(Int(app.state.fontSize))").monospacedDigit() }
            Slider(value: Binding(get: { app.state.fontSize }, set: { value in app.update { $0.fontSize = value } }),
                   in: 16...34, step: 1) { Text("Tamaño de lectura") }
                .accessibilityValue("\(Int(app.state.fontSize)) puntos")
        }.padding(.vertical, 8)
    }
}

struct VersionPicker: View {
    @EnvironmentObject private var app: AppModel
    @Environment(\.dismiss) private var dismiss
    var selected: String? = nil
    var location: ReadingLocation? = nil
    let onSelect: (String) -> Void
    var body: some View {
        NavigationStack {
            List {
                Section {
                    ForEach(app.versions) { version in
                        let available = location.map { app.isAvailable($0, version: version.id) } ?? true
                        Button {
                            onSelect(version.id)
                            dismiss()
                        } label: {
                            HStack(spacing: 16) {
                                VStack(alignment: .leading, spacing: 6) {
                                    Text(version.abbreviation).font(.headline)
                                    Text(version.name).font(.subheadline).foregroundStyle(BiblionTheme.secondary)
                                    if !available {
                                        Text("Este capítulo no está disponible").font(.caption).foregroundStyle(BiblionTheme.secondary)
                                    } else if version.bookCount < 66 {
                                        Text("Contenido parcial · \(version.bookCount) libros").font(.caption).foregroundStyle(BiblionTheme.secondary)
                                    }
                                }
                                Spacer(minLength: 8)
                                if version.id == (selected ?? app.state.selectedVersion) {
                                    Image(systemName: "checkmark").foregroundStyle(BiblionTheme.blue).accessibilityLabel("Seleccionada")
                                }
                            }.padding(.vertical, 8)
                        }.disabled(!available).listRowBackground(BiblionTheme.surface)
                    }
                } footer: { Text("Todas estas versiones están incluidas para leer sin internet.") }
            }
            .navigationTitle("Versión bíblica").navigationBarTitleDisplayMode(.inline)
            .scrollContentBackground(.hidden)
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Cerrar") { dismiss() } } }
            .biblionPage()
        }
    }
}
#endif
