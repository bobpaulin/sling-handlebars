
function compileAndRenderTemplate(templateSrc, data)
{
	var template = Handlebars.compile(templateSrc);
	return template(data);
}

function renderTemplate(templateFunction, data)
{
	return templateFunction(data);
}