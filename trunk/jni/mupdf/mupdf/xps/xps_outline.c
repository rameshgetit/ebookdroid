#include "fitz.h"
#include "muxps.h"

/*
 * Parse the document structure / outline parts referenced from fixdoc relationships.
 */

static fz_outline *
xps_find_last_outline_at_level(fz_outline *node, int level, int target_level)
{
	while (node->next)
		node = node->next;
	if (level == target_level || !node->down)
		return node;
	return xps_find_last_outline_at_level(node->down, level + 1, target_level);
}

/* SumatraPDF: extended outline actions */
#define isprotc(c) (('A' <= (c) && (c) <= 'Z') || ('a' <= (c) && (c) <= 'z') || (c) == '-')

static int
xps_is_external_uri(char *path)
{
	char *c;
	for (c = path; isprotc(*c); c++);
	return c > path && *c == ':';
}

static fz_outline *
xps_parse_document_outline(xps_document *doc, xml_element *root)
{
	xml_element *node;
	fz_outline *head = NULL, *entry, *tail;
	int last_level = 1, this_level;
	for (node = xml_down(root); node; node = xml_next(node))
	{
		if (!strcmp(xml_tag(node), "OutlineEntry"))
		{
			char *level = xml_att(node, "OutlineLevel");
			char *target = xml_att(node, "OutlineTarget");
			char *description = xml_att(node, "Description");
			/* SumatraPDF: allow target-less outline entries */
			if (!description)
				continue;

			entry = fz_malloc_struct(doc->ctx, fz_outline);
			entry->title = fz_strdup(doc->ctx, description);
			entry->page = xps_find_link_target(doc, target);
			entry->down = NULL;
			entry->next = NULL;

			this_level = level ? atoi(level) : 1;

			if (!head)
			{
				head = entry;
			}
			else
			{
				tail = xps_find_last_outline_at_level(head, 1, this_level);
				if (this_level > last_level)
					tail->down = entry;
				else
					tail->next = entry;
			}

			last_level = this_level;
		}
	}
	return head;
}

static fz_outline *
xps_parse_document_structure(xps_document *doc, xml_element *root)
{
	xml_element *node;
	if (!strcmp(xml_tag(root), "DocumentStructure"))
	{
		node = xml_down(root);
		if (!strcmp(xml_tag(node), "DocumentStructure.Outline"))
		{
			node = xml_down(node);
			if (!strcmp(xml_tag(node), "DocumentOutline"))
				return xps_parse_document_outline(doc, node);
		}
	}
	return NULL;
}

static fz_outline *
xps_load_document_structure(xps_document *doc, xps_fixdoc *fixdoc)
{
	xps_part *part;
	xml_element *root;
	fz_outline *outline;

	part = xps_read_part(doc, fixdoc->outline);
	fz_try(doc->ctx)
	{
		root = xml_parse_document(doc->ctx, part->data, part->size);
	}
	fz_catch(doc->ctx)
	{
		xps_free_part(doc, part);
		fz_rethrow(doc->ctx);
	}
	xps_free_part(doc, part);

	fz_try(doc->ctx)
	{
		outline = xps_parse_document_structure(doc, root);
	}
	fz_catch(doc->ctx)
	{
		xml_free_element(doc->ctx, root);
		fz_rethrow(doc->ctx);
	}
	xml_free_element(doc->ctx, root);

	return outline;
}

fz_outline *
xps_load_outline(xps_document *doc)
{
	xps_fixdoc *fixdoc;
	fz_outline *head = NULL, *tail, *outline;

	for (fixdoc = doc->first_fixdoc; fixdoc; fixdoc = fixdoc->next) {
		if (fixdoc->outline) {
			outline = xps_load_document_structure(doc, fixdoc);
			/* SumatraPDF: don't overwrite outline entries */
			if (head)
			{
				while (tail->next)
					tail = tail->next;
			}
			if (!head)
				head = outline;
			else
				tail->next = outline;
			tail = outline;
		}
	}
	return head;
}

/* SumatraPDF: extended link support */

void
xps_extract_anchor_info(xps_document *doc, xml_element *node, fz_rect rect)
{
	char *value;

	if (doc->link_root && (value = xml_att(node, "FixedPage.NavigateUri")))
	{
		fz_link *link;
		fz_link_dest ld;
		ld.uri.uri = fz_strdup(doc->ctx, value);
		link = fz_new_link(doc->ctx, FZ_LINK_URI, rect, ld);
		// insert the links in bottom-to-top order (first one is to be preferred)
		link->next = doc->link_root->next;
		doc->link_root->next = link;
	}

	if ((value = xml_att(node, "Name")))
	{
		xps_target *target;
		char *valueId = fz_malloc(doc->ctx, strlen(value) + 2);
		sprintf(valueId, "#%s", value);
		target = xps_find_link_target_obj(doc, valueId);
		if (target)
			target->rect = rect;
		fz_free(doc->ctx, valueId);
	}
}

/* SumatraPDF: extract document properties (hacky) */

static xml_element *
xps_open_and_parse(xps_document *doc, char *path)
{
	xml_element *root = NULL;
	xps_part *part = xps_read_part(doc, path);
	/* "cannot read part '%s'", path */;

	fz_try(doc->ctx)
	{
		root = xml_parse_document(doc->ctx, part->data, part->size);
		xps_free_part(doc, part);
	}
	fz_catch(doc->ctx)
	{
		xps_free_part(doc, part);
		fz_rethrow(doc->ctx);
	}

	return root;
}

static inline int iswhite(c) { return c == ' ' || c == '\t' || c == '\n' || c == '\r'; }

static void
xps_hacky_get_prop(fz_context *ctx, char *data, fz_obj *dict, char *name, char *tag_name)
{
	char *start, *end;
	fz_obj *value;

	start = strstr(data, tag_name);
	if (!start || start == data || start[-1] != '<')
		return;
	end = strstr(start + 1, tag_name);
	start = strchr(start, '>');
	if (!start || !end || start >= end || end[-2] != '<' || end[-1] != '/')
		return;

	for (start++; iswhite(*start); start++);
	for (end -= 3; iswhite(*end) && end > start; end--);

	value = fz_new_string(ctx, start, end - start + 1);
	fz_dict_puts(dict, name, value);
	fz_drop_obj(value);
}

#define REL_CORE_PROPERTIES \
	"http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties"

static void
xps_find_doc_props_path(xps_document *doc, char path[1024])
{
	xml_element *root = xps_open_and_parse(doc, "/_rels/.rels");
	xml_element *item;

	if (strcmp(xml_tag(root), "Relationships") != 0)
		fz_throw(doc->ctx, "couldn't parse part '/_rels/.rels'");

	*path = '\0';
	for (item = xml_down(root); item; item = xml_next(item))
	{
		if (!strcmp(xml_tag(item), "Relationship") && xml_att(item, "Type") &&
			!strcmp(xml_att(item, "Type"), REL_CORE_PROPERTIES) && xml_att(item, "Target"))
		{
			xps_absolute_path(path, "", xml_att(item, "Target"), 1024);
		}
	}

	xml_free_element(doc->ctx, root);
}

fz_obj *xps_extract_doc_props(xps_document *doc)
{
	char path[1024];
	xps_part *part;
	fz_obj *dict = NULL;

	fz_var(dict);

	fz_try(doc->ctx)
	{
		xps_find_doc_props_path(doc, path);
	}
	fz_catch(doc->ctx)
	{
		fz_warn(doc->ctx, "ignore broken part '/_rels/.rels'");
		return NULL;
	}
	if (!*path)
		return NULL;

	fz_try(doc->ctx)
	{
		part = xps_read_part(doc, path);
	}
	fz_catch(doc->ctx)
	{
		fz_warn(doc->ctx, "cannot read zip part '%s'", path);
		return NULL;
	}

	fz_try(doc->ctx)
	{
		dict = fz_new_dict(doc->ctx, 8);
		xps_hacky_get_prop(doc->ctx, part->data, dict, "Title", "dc:title");
		xps_hacky_get_prop(doc->ctx, part->data, dict, "Subject", "dc:subject");
		xps_hacky_get_prop(doc->ctx, part->data, dict, "Author", "dc:creator");
		xps_hacky_get_prop(doc->ctx, part->data, dict, "CreationDate", "dcterms:created");
		xps_hacky_get_prop(doc->ctx, part->data, dict, "ModDate", "dcterms:modified");
	}
	fz_catch(doc->ctx)
	{
		fz_drop_obj(dict);
		dict = NULL;
		fz_warn(doc->ctx, "cannot read zip part '%s'", path);
	}

	xps_free_part(doc, part);

	return dict;
}
