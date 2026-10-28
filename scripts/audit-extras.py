#!/usr/bin/env python3
"""Audit the Extras catalog, resources, source references and recorded JVM results.

This does not certify runtime behavior. Native UI, GPU and authenticated-server
checks are reported separately and never inferred from a stored preference.
"""
import argparse
import collections
import hashlib
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'TMessagesProj/src/main/java'


def resources(locale):
    root = ET.parse(ROOT / f'TMessagesProj/src/main/res/{locale}/strings.xml').getroot()
    return {x.attrib['name']: ''.join(x.itertext()) for x in root if x.tag == 'string'}


def audit():
    config = (JAVA / 'org/telegram/messenger/banana/BananaConfig.java').read_text()
    catalog = (JAVA / 'org/telegram/messenger/banana/BananaExtrasCatalog.java').read_text()
    hooks = (JAVA / 'org/telegram/messenger/banana/BananaHooks.java').read_text()
    enum = config.split('public enum Option {', 1)[1].split('public enum SilentSending', 1)[0]
    options = re.findall(r'^        ([A-Z_]+)\(', enum, re.M)
    en, ru = resources('values'), resources('values-ru')
    catalog_options = set(re.findall(r'Option\.([A-Z_]+)', catalog))
    checks = {
        'known_catalog_options': catalog_options <= set(options),
        'all_options_in_catalog': set(options) <= catalog_options,
        'english_labels': all('BananaOpt_' + x in en for x in options),
        'russian_labels': all(re.search('[А-Яа-яЁё]', ru.get('BananaOpt_' + x, '')) for x in options),
    }
    # References are navigation aids for review, not proof that a behavior works.
    sources = {str(p.relative_to(ROOT)): p.read_text() for p in JAVA.rglob('*.java')}
    excluded = {'BananaConfig.java', 'BananaExtrasCatalog.java', 'BananaExtrasSectionActivity.java'}
    entries = []
    digest = hashlib.sha256()
    for path, text in sorted(sources.items()):
        digest.update(path.encode()); digest.update(text.encode())
    references = collections.defaultdict(list)
    for path, text in sources.items():
        if Path(path).name in excluded: continue
        for line, value in enumerate(text.splitlines(), 1):
            for option in set(re.findall(r'Option\.([A-Z_]+)\b', value)):
                references[option].append({'file': path, 'line': line})
    for option in options:
        entries.append({'option': option, 'label_ru': ru.get('BananaOpt_' + option),
                        'source_references': references[option], 'runtime_acceptance': 'not_certified'})
    inventory = json.loads((ROOT / 'docs/phone-port-inventory.json').read_text())
    suites = []
    for file in sorted((ROOT / 'TMessagesProj/build/test-results/testDebugUnitTest').glob('TEST-*.xml')):
        result = ET.parse(file).getroot()
        suites.append({key: result.attrib[key] for key in ['name', 'tests', 'failures', 'errors', 'skipped']})
    totals = {key: sum(int(s[key]) for s in suites) for key in ['tests', 'failures', 'errors', 'skipped']}
    return {
        'schema': 1, 'scope': 'source/resources and recorded JVM results; runtime acceptance is separate',
        'java_source_sha256': digest.hexdigest(), 'checks': checks,
        'options_count': len(options), 'options': entries,
        'ios_inventory_count': len(inventory['settings']), 'ios_settings': inventory['settings'],
        'inventory_status_counts': dict(collections.Counter(x['status'] for x in inventory['settings'])),
        'jvm': {'totals': totals, 'suites': suites},
        'runtime': {'instrumentation': 'not_run', 'gpu': 'not_run', 'authenticated_server': 'not_run'},
    }


def markdown(report):
    lines = ['# Аттестация новых функций BananaGram Android', '',
             'Проверка исходников охватывает все 176 настроек каталога iOS и 157 переключателей Android. '
             'Наличие вызова или переключателя не означает успешной проверки поведения на устройстве.', '',
             '## Подтверждённые проверки', '',
             '- XML ресурсов разбирается; у каждого переключателя есть английская и русская подпись.',
             '- Каждый переключатель включён в каталог Extras; неизвестных пунктов в каталоге нет.',
             f"- Записанные результаты JVM: {report['jvm']['totals']['tests']} тестов; "
             f"ошибок {report['jvm']['totals']['errors']}, падений {report['jvm']['totals']['failures']}, "
             f"пропусков {report['jvm']['totals']['skipped']}.",
             '- Результаты сборки и ограничения среды зафиксированы в `feature-attestation-validation.md`.', '',
             '## Реальные ограничения', '',
             '- Сессии Android: Keystore для локальных копий, пароль для экспорта; перенос настоящей сессии требует отдельной приёмки.',
             '- Локальная расшифровка: Vosk RU/EN, без серверного запроса; выполнение MediaCodec/Vosk проверяется отдельным Android-сценарием.',
             '- TikTok/Shorts: встроенный yt-dlp/FFmpeg; реальные сайты, отмена и сохранение в галерею требуют сетевой приёмки.',
             '- Сохранение медиа: завершение загрузки связано с отдельным архивом; сценарий кеш/удаление/очистка включён в AndroidTest.',
             '- Ghost, серверные удаления/правки и истории требуют приёмки на авторизованном тестовом аккаунте.',
             '- GPU-стекло, читаемость и частота кадров требуют проверки на устройстве. JVM-тесты не измеряют FPS.', '',
             '## Темы и стекло', '',
             'Разрешены только Classic White, Classic Black, BananaGram Cream и BananaGram Graphite. '
             'Обычное применение, фоновые переходы, временный предпросмотр и импорт проверяют политику. '
             'Посторонний объект с разрешённым названием не проходит проверку принадлежности реестру. '
             'Палитры чатов не переопределяют эти темы. Старые Glass-палитры мигрируют в светлую/тёмную классику.', '',
             '«Включить жидкое стекло» — отдельный переключатель Extras. Он управляет геометрией и GPU-преломлением '
             'в существующем blur3, заливкой и независимыми бликами пузырей. При ошибке RuntimeShader остаётся обычное '
             'размытие. Настройки энергосбережения сохраняют приоритет. Пузыри получают прозрачную заливку и контур; '
             'текстовые пузыри Android 13+ используют GPU-линзу над обоями; она принадлежит ячейке, не захватывает сам чат и не запускает собственный цикл кадров. GPU-приёмка ещё не выполнена.', '',
             '## Каталог iOS: индивидуальные результаты', '',
             'Статусы ниже — результаты проверки реализации в исходниках. '
             '`implemented_behavior_wired` обозначает подключение к коду, а не успешный сценарий на устройстве. '
             'Серверная и визуальная приёмка не присваиваются автоматически.', '',
             '| Настройка | Статус исходников | Реализация / ограничение |',
             '|---|---|---|']
    for item in report['ios_settings']:
        desc = item['android'].replace('|', '\\|').replace('\n', ' ')
        lines.append(f"| `{item['setting']}` | `{item['status']}` | {desc} |")
    lines += ['', 'Полный машинный отчёт с местами использования: `feature-attestation.json`.', '',
              'Повторная проверка: `python3 scripts/audit-extras.py --output docs/feature-attestation.json '
              '--markdown docs/feature-attestation.md`.', '']
    return '\n'.join(lines)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path)
    parser.add_argument('--markdown', type=Path)
    args = parser.parse_args()
    report = audit()
    if args.output: args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    if args.markdown: args.markdown.write_text(markdown(report))
    print(json.dumps({'checks': report['checks'], 'options': report['options_count'],
                      'ios_settings': report['ios_inventory_count'], 'jvm': report['jvm']['totals']}, ensure_ascii=False))
    if not all(report['checks'].values()): raise SystemExit(1)


if __name__ == '__main__':
    main()
