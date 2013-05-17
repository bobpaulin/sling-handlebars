
function renderTemplate(templateSrc, data)
{
	var template = Handlebars.compile(templateSrc);
	return template(data);
}