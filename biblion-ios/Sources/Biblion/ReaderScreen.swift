#if canImport(SwiftUI)
import SwiftUI
import UIKit
import BiblionCore

struct ReaderScreen: View {
    @EnvironmentObject private var app: AppModel
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @StateObject private var reader: ReaderModel
    @State private var showVersions = false
    @State private var showAppearance = false
    @State private var showChapters = false
    @State private var visibleVerse: Int?
    @State private var retry = 0
    @State private var showBibi = false
    @State private var referenceLocation: ReadingLocation?
    @State private var compareLocation: ReadingLocation?

    init(location: ReadingLocation) { _reader = StateObject(wrappedValue: ReaderModel(location: location)) }
    private var versionLabel: String { app.version(reader.location.version)?.abbreviation ?? reader.location.version.uppercased() }
    private var book: BibleBook? { app.books(version: reader.location.version).first { $0.id == reader.location.book } }
    private var previous: ReadingLocation? {
        ChapterNavigation.adjacent(to: reader.location, in: app.books(version: reader.location.version), forward: false)
    }
    private var next: ReadingLocation? {
        ChapterNavigation.adjacent(to: reader.location, in: app.books(version: reader.location.version), forward: true)
    }
    var body: some View {
        Group {
            if reader.isLoading {
                ProgressView("Abriendo capítulo…").frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let error = reader.error {
                StatusView(title: "No pudimos abrir el capítulo", message: error, symbol: "book.closed",
                           actionTitle: "Reintentar") { retry += 1 }
            } else if let chapter = reader.chapter {
                chapterContent(chapter)
            }
        }
        .navigationTitle(BibleBook.displayName(for: reader.location.book))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { showBibi = true } label: { Image(systemName: "bubble.left.and.bubble.right").frame(minWidth: 44, minHeight: 44) }.accessibilityLabel("Abrir Bibi")
            }
            ToolbarItem(placement: .topBarTrailing) {
                VersionButton(title: versionLabel) { showVersions = true }
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button { showAppearance = true } label: {
                    Image(systemName: "textformat.size").frame(minWidth: 44, minHeight: 44)
                }.accessibilityLabel("Ajustes de lectura")
            }
        }
        .safeAreaInset(edge: .bottom, spacing: 0) { chapterControls }
        .sheet(isPresented: $showVersions) {
            VersionPicker(selected: reader.location.version, location: reader.location) { version in
                app.selectVersion(version)
                reader.location.version = version
            }
        }
        .sheet(isPresented: $showAppearance) { ReaderAppearanceSheet() }
        .sheet(isPresented: $showBibi) { NavigationStack { BibiScreen(location: reader.location) } }
        .sheet(isPresented: Binding(get: { compareLocation != nil }, set: { if !$0 { compareLocation = nil } })) {
            if let location = compareLocation { NavigationStack { CompareVersionsScreen(location: location) } }
        }
        .sheet(isPresented: Binding(get: { referenceLocation != nil }, set: { if !$0 { referenceLocation = nil } })) {
            if let location = referenceLocation { NavigationStack { CrossReferencesScreen(location: location).biblionDestinations() } }
        }
        .sheet(isPresented: $showChapters) {
            ReaderChapterPicker(book: book, selected: reader.location.chapter) { chapter in
                reader.location = ReadingLocation(version: reader.location.version, book: reader.location.book, chapter: chapter)
            }
        }
        .task(id: LoadKey(location: reader.location, retry: retry)) {
            visibleVerse = nil
            await reader.load(using: app)
            guard !Task.isCancelled, let verses = reader.chapter?.verses else { return }
            let preferred = reader.location.verse
            visibleVerse = verses.first(where: { $0.number == preferred })?.number ?? verses.first?.number
        }
        .onChange(of: visibleVerse) { _, number in
            guard let number, !reader.isLoading, reader.chapter?.verses.contains(where: { $0.number == number }) == true else { return }
            var position = reader.location
            position.verse = number
            app.remember(position)
        }
        .biblionPage()
    }

    private func chapterContent(_ chapter: BibleChapter) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                VStack(alignment: .leading, spacing: 10) {
                    Text(versionLabel).font(.subheadline.weight(.semibold)).foregroundStyle(BiblionTheme.gold)
                    Text("Capítulo \(reader.location.chapter)")
                        .font(.system(.largeTitle, design: .serif, weight: .semibold)).accessibilityAddTraits(.isHeader)
                    Text("Mantén pulsado un versículo para resaltarlo, copiarlo o compartirlo.")
                        .font(.footnote).foregroundStyle(BiblionTheme.secondary)
                }.padding(.horizontal, 12)
                LazyVStack(alignment: .leading, spacing: 12) {
                    ForEach(chapter.verses) { verse in
                        verseRow(verse).id(verse.number)
                    }
                }.scrollTargetLayout()
            }.padding(.horizontal, 16).padding(.top, 28).padding(.bottom, 32)
                .frame(maxWidth: 760).frame(maxWidth: .infinity)
        }
        .scrollPosition(id: $visibleVerse, anchor: .top)
        .simultaneousGesture(DragGesture(minimumDistance: 60).onEnded { value in
            guard abs(value.translation.width) > abs(value.translation.height) * 2 else { return }
            if value.translation.width < 0, let next { reader.location = next }
            else if value.translation.width > 0, let previous { reader.location = previous }
        })
        .id("\(chapter.location.version):\(chapter.location.book):\(chapter.location.chapter)")
    }

    private func verseRow(_ verse: BibleVerse) -> some View {
        let location = ReadingLocation(version: reader.location.version, book: reader.location.book,
                                       chapter: reader.location.chapter, verse: verse.number)
        let highlighted = app.highlighted(location)
        return VStack(alignment: .leading, spacing: 12) {
            if let title = verse.title, !title.isEmpty {
                Text(title).font(.headline).padding(.top, 16).accessibilityAddTraits(.isHeader)
            }
            HStack(alignment: .firstTextBaseline, spacing: 12) {
                VStack(spacing: 6) {
                    Text("\(verse.number)").font(.caption.weight(.semibold).monospacedDigit())
                    if highlighted { Image(systemName: "bookmark.fill").font(.caption2).accessibilityHidden(true) }
                }.foregroundStyle(BiblionTheme.gold).frame(minWidth: 24)
                Text(verse.text).font(.custom("Georgia", size: app.state.fontSize, relativeTo: .body))
                    .lineSpacing(8).frame(maxWidth: .infinity, alignment: .leading)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(12).frame(maxWidth: .infinity, alignment: .leading)
            .background(highlighted ? highlightColor(location) : .clear, in: RoundedRectangle(cornerRadius: 10))
            .contentShape(Rectangle())
            .contextMenu {
                Button(highlighted ? "Quitar resaltado" : "Resaltar", systemImage: highlighted ? "bookmark.slash" : "bookmark") {
                    app.toggleHighlight(location, text: verse.text)
                }
                Menu("Color del resaltado") {
                    ForEach([("Dorado", UInt32(0xFFFFD54F)), ("Azul", UInt32(0xFF81D4FA)), ("Verde", UInt32(0xFFA5D6A7)), ("Rosa", UInt32(0xFFF48FB1))], id: \.0) { item in
                        Button(item.0) { app.highlight(location, text: verse.text, color: item.1) }
                    }
                }
                Button("Copiar", systemImage: "doc.on.doc") { UIPasteboard.general.string = shareText(verse, location: location) }
                ShareLink(item: shareText(verse, location: location)) { Label("Compartir", systemImage: "square.and.arrow.up") }
                Button("Referencias relacionadas", systemImage: "arrow.triangle.branch") { referenceLocation = location }
                Button("Comparar versiones", systemImage: "rectangle.split.2x1") { compareLocation = location }
            }
            .accessibilityElement(children: .combine)
            .accessibilityLabel("Versículo \(verse.number). \(verse.text)")
            .accessibilityValue(highlighted ? "Resaltado" : "")
            .accessibilityAction(named: Text(highlighted ? "Quitar resaltado" : "Resaltar")) {
                app.toggleHighlight(location, text: verse.text)
            }
        }
    }

    private func shareText(_ verse: BibleVerse, location: ReadingLocation) -> String {
        "\(verse.text)\n\n\(location.reference) · \(versionLabel)\nBiblion"
    }
    private func highlightColor(_ location: ReadingLocation) -> Color {
        guard let color = app.state.highlights.first(where: { $0.location == location })?.color else { return BiblionTheme.highlight }
        return Color(red: Double((color >> 16) & 255) / 255, green: Double((color >> 8) & 255) / 255, blue: Double(color & 255) / 255).opacity(0.3)
    }

    private var chapterControls: some View {
        Group {
            if dynamicTypeSize.isAccessibilitySize {
                VStack(spacing: 0) {
                    chapterPickerButton
                    HStack { previousButton; Spacer(); nextButton }
                }
            } else {
                HStack(spacing: 12) {
                    previousButton
                    Spacer(minLength: 0)
                    chapterPickerButton
                    Spacer(minLength: 0)
                    nextButton
                }
            }
        }
        .padding(.horizontal, 12).padding(.vertical, 4)
        .background(BiblionTheme.surface)
        .overlay(alignment: .top) { Rectangle().fill(BiblionTheme.divider).frame(height: 1) }
    }

    private var chapterPickerButton: some View {
        Button { showChapters = true } label: {
            Label("Capítulo \(reader.location.chapter)", systemImage: "square.grid.2x2")
                .font(.subheadline.weight(.semibold)).frame(minHeight: 48)
        }.disabled(book == nil).accessibilityHint("Abre el selector de capítulos")
    }
    private var previousButton: some View {
        Button { if let previous { reader.location = previous } } label: {
            Image(systemName: "chevron.left").frame(minWidth: 48, minHeight: 48)
        }.disabled(previous == nil || reader.isLoading).accessibilityLabel("Capítulo anterior")
    }
    private var nextButton: some View {
        Button { if let next { reader.location = next } } label: {
            Image(systemName: "chevron.right").frame(minWidth: 48, minHeight: 48)
        }.disabled(next == nil || reader.isLoading).accessibilityLabel("Capítulo siguiente")
    }

    private struct LoadKey: Hashable { let location: ReadingLocation; let retry: Int }
}

struct ReaderAppearanceSheet: View {
    @EnvironmentObject private var app: AppModel
    @Environment(\.dismiss) private var dismiss
    var body: some View {
        NavigationStack {
            Form {
                Section("Texto") { FontSizeControl() }
                Section("Tema") {
                    Picker("Apariencia", selection: Binding(get: { app.state.appearance }, set: { value in app.update { $0.appearance = value } })) {
                        ForEach(AppAppearance.allCases, id: \.self) { Text($0.title).tag($0) }
                    }.pickerStyle(.inline).labelsHidden()
                }
            }
            .navigationTitle("Ajustes de lectura").navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Listo") { dismiss() } } }
            .scrollContentBackground(.hidden).biblionPage()
        }.presentationDetents([.medium, .large])
    }
}

struct ReaderChapterPicker: View {
    @Environment(\.dismiss) private var dismiss
    let book: BibleBook?
    let selected: Int
    let onSelect: (Int) -> Void
    @ScaledMetric private var width: CGFloat = 64
    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVGrid(columns: [GridItem(.adaptive(minimum: width), spacing: 12)], spacing: 12) {
                    ForEach(book?.chapters ?? [], id: \.self) { chapter in
                        Button { onSelect(chapter); dismiss() } label: {
                            Text("\(chapter)").font(.title3.monospacedDigit())
                                .frame(maxWidth: .infinity, minHeight: width)
                                .background(chapter == selected ? BiblionTheme.highlight : BiblionTheme.surface,
                                            in: RoundedRectangle(cornerRadius: 12))
                                .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(BiblionTheme.divider))
                        }
                        .accessibilityLabel("Capítulo \(chapter)")
                        .accessibilityAddTraits(chapter == selected ? .isSelected : [])
                    }
                }.padding(20)
            }
            .navigationTitle(book?.name ?? "Capítulos").navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Cerrar") { dismiss() } } }
            .biblionPage()
        }
    }
}
#endif
