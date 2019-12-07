/* ***** BEGIN LICENSE BLOCK *****
 * Distributed under the BSD license:
 *
 * Copyright (c) 2010, Ajax.org B.V.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Ajax.org B.V. nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL AJAX.ORG B.V. BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ***** END LICENSE BLOCK ***** */

define("ace/snippets/markdown",["require","exports","module"], function(require, exports, module) {
"use strict";

exports.snippetText = "# Markdown\n\
\n\
# Includes octopress (http://octopress.org/) snippets\n\
\n\
snippet [\n\
	[${1:text}](http://${2:address} \"${3:title}\")\n\
snippet [*\n\
	[${1:link}](${2:`@*`} \"${3:title}\")${4}\n\
\n\
snippet [:\n\
	[${1:id}]: http://${2:url} \"${3:title}\"\n\
snippet [:*\n\
	[${1:id}]: ${2:`@*`} \"${3:title}\"\n\
\n\
snippet ![\n\
	![${1:alttext}](${2:/images/image.jpg} \"${3:title}\")\n\
snippet ![*\n\
	![${1:alt}](${2:`@*`} \"${3:title}\")${4}\n\
\n\
snippet ![:\n\
	![${1:id}]: ${2:url} \"${3:title}\"\n\
snippet ![:*\n\
	![${1:id}]: ${2:`@*`} \"${3:title}\"\n\
\n\
snippet ===\n\
regex /^/=+/=*//\n\
	${PREV_LINE/./=/g}\n\
	\n\
	${0}\n\
snippet ---\n\
regex /^/-+/-*//\n\
	${PREV_LINE/./-/g}\n\
	\n\
	${0}\n\
snippet blockquote\n\
	{% blockquote %}\n\
	${1:quote}\n\
	{% endblockquote %}\n\
\n\
snippet blockquote-author\n\
	{% blockquote ${1:author}, ${2:title} %}\n\
	${3:quote}\n\
	{% endblockquote %}\n\
\n\
snippet blockquote-link\n\
	{% blockquote ${1:author} ${2:URL} ${3:link_text} %}\n\
	${4:quote}\n\
	{% endblockquote %}\n\
\n\
snippet bt-codeblock-short\n\
	```\n\
	${1:code_snippet}\n\
	```\n\
\n\
snippet bt-codeblock-full\n\
	``` ${1:language} ${2:title} ${3:URL} ${4:link_text}\n\
	${5:code_snippet}\n\
	```\n\
\n\
snippet codeblock-short\n\
	{% codeblock %}\n\
	${1:code_snippet}\n\
	{% endcodeblock %}\n\
\n\
snippet codeblock-full\n\
	{% codeblock ${1:title} lang:${2:language} ${3:URL} ${4:link_text} %}\n\
	${5:code_snippet}\n\
	{% endcodeblock %}\n\
\n\
snippet gist-full\n\
	{% gist ${1:gist_id} ${2:filename} %}\n\
\n\
snippet gist-short\n\
	{% gist ${1:gist_id} %}\n\
\n\
snippet img\n\
	{% img ${1:class} ${2:URL} ${3:width} ${4:height} ${5:title_text} ${6:alt_text} %}\n\
\n\
snippet youtube\n\
	{% youtube ${1:video_id} %}\n\
\n\
# The quote should appear only once in the text. It is inherently part of it.\n\
# See http://octopress.org/docs/plugins/pullquote/ for more info.\n\
\n\
snippet pullquote\n\
	{% pullquote %}\n\
	${1:text} {\" ${2:quote} \"} ${3:text}\n\
	{% endpullquote %}\n\
";
exports.scope = "markdown";

});
