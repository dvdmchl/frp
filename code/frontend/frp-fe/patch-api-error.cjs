// patch-api-error.js
const fs = require('fs');
const path = require('path');

const filePath = path.resolve(__dirname, 'src/api/core/ApiError.ts');

fs.readFile(filePath, 'utf8', (err, data) => {
    if (err) {
        console.error('Error reading ApiError.ts:', err);
        return;
    }

    const importStatement = "import type { ErrorDto } from '../models/ErrorDto';";
    const propertyDeclaration = "    public readonly errorDto: ErrorDto | undefined;";
    const constructorLogic = `
        if (this.body && typeof this.body === 'object' && ('message' in this.body || 'type' in this.body || 'stackTrace' in this.body)) {
            this.errorDto = this.body as ErrorDto;
            this.message = this.errorDto.message || message;
        }
    `;

    let updatedContent = data;

    // Add import statement if not present
    if (!updatedContent.includes(importStatement)) {
        updatedContent = updatedContent.replace(
            "import type { ApiResult } from './ApiResult';",
            `import type { ApiResult } from './ApiResult';\n${importStatement}`
        );
    }

    // Add property declaration if not present
    if (!updatedContent.includes(propertyDeclaration)) {
        updatedContent = updatedContent.replace(
            "    public readonly request: ApiRequestOptions;",
            `    public readonly request: ApiRequestOptions;\n${propertyDeclaration}`
        );
    }

    // Add constructor logic if not present
    if (!updatedContent.includes("if (this.body && typeof this.body === 'object'")) {
        updatedContent = updatedContent.replace(
            "        this.request = request;",
            `        this.request = request;${constructorLogic}`
        );
    }

    fs.writeFile(filePath, updatedContent, 'utf8', (err) => {
        if (err) {
            console.error('Error writing to ApiError.ts:', err);
        } else {
            console.log('ApiError.ts patched successfully.');
        }
    });
});
