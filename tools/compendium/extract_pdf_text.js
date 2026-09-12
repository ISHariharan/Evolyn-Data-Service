// Prints the text of a PDF, page by page, using macOS's built-in PDFKit
// (no extra installs needed). Run with:
//
//   osascript -l JavaScript tools/compendium/extract_pdf_text.js /path/to/compendium.pdf > compendium.txt
//
// The output feeds build_catalogue.py.
ObjC.import('PDFKit');

function run(argv) {
  if (argv.length < 1) return 'usage: extract_pdf_text.js <file.pdf>';
  const url = $.NSURL.fileURLWithPath(argv[0]);
  const doc = $.PDFDocument.alloc.initWithURL(url);
  if (doc.isNil()) return 'Could not open ' + argv[0];
  let out = '';
  for (let i = 0; i < doc.pageCount; i++) {
    out += '\n=====PAGE ' + (i + 1) + '=====\n' + ObjC.unwrap(doc.pageAtIndex(i).string);
  }
  return out;
}
