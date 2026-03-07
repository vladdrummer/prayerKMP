const fs = require("fs");
const path = require("path");

const sourceFiles = [
  "C:/git-shared/prayer/Prayer/app/src/main/res/values/strings.xml",
  "C:/git-shared/prayer/Prayer/app/src/main/res/values/thirdrelease.xml",
  "C:/git-shared/prayer/Prayer/app/src/main/res/values/saints.xml",
];

const targetFile =
  "C:/git-shared/prayerkmp/composeApp/src/commonMain/composeResources/files/prayer_strings_merged.xml";

function stripResourcesWrapper(xmlText) {
  return xmlText
    .replace(/^\uFEFF/, "")
    .replace(/<\?xml[\s\S]*?\?>/i, "")
    .replace(/^\s*<resources>\s*/i, "")
    .replace(/\s*<\/resources>\s*$/i, "")
    .trim();
}

const mergedBody = sourceFiles
  .map((sourcePath) => {
    const fileText = fs.readFileSync(sourcePath, "utf8");
    return stripResourcesWrapper(fileText);
  })
  .join("\n\n");

const mergedXml =
  `<?xml version="1.0" encoding="utf-8"?>\n<resources>\n` +
  mergedBody +
  `\n</resources>\n`;

fs.mkdirSync(path.dirname(targetFile), { recursive: true });
fs.writeFileSync(targetFile, mergedXml, "utf8");

console.log(`Merged ${sourceFiles.length} files into: ${targetFile}`);
console.log(`Output size: ${mergedXml.length} chars`);
