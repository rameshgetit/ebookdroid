#include "fitz.h"

#ifdef __ANDROID__
#define EXIT(code) do {} while(0)
#else
#define EXIT(code) exit(code)
#endif


/* Warning context */

void fz_var_imp(void *var)
{
	var = var; /* Do nothing */
}

void fz_flush_warnings(fz_context *ctx)
{
	if (ctx->warn->count > 1)
	{
		LOGE("warning: ... repeated %d times ...\n", ctx->warn->count);
	}
	ctx->warn->message[0] = 0;
	ctx->warn->count = 0;
}

void fz_warn(fz_context *ctx, char *fmt, ...)
{
	va_list ap;
	char buf[sizeof ctx->warn->message];

	va_start(ap, fmt);
	vsnprintf(buf, sizeof buf, fmt, ap);
	va_end(ap);

	if (!strcmp(buf, ctx->warn->message))
	{
		ctx->warn->count++;
	}
	else
	{
		fz_flush_warnings(ctx);
		LOGE("warning: %s\n", buf);
		fz_strlcpy(ctx->warn->message, buf, sizeof ctx->warn->message);
		ctx->warn->count = 1;
	}
}

/* Error context */

static void throw(fz_error_context *ex)
{
	if (ex->top >= 0) {
		longjmp(ex->stack[ex->top].buffer, 1);
	} else {
		LOGE("uncaught exception: %s\n", ex->message);
		EXIT(EXIT_FAILURE);
	}
}

void fz_push_try(fz_error_context *ex)
{
	assert(ex);
	if (ex->top + 1 >= nelem(ex->stack))
	{
    		LOGE("exception stack overflow: %d!\n", ex->top);
		EXIT(EXIT_FAILURE);
	}
	ex->top++;
}

char *fz_caught(fz_context *ctx)
{
	assert(ctx);
	assert(ctx->error);
	return ctx->error->message;
}

void fz_throw(fz_context *ctx, char *fmt, ...)
{
	va_list args;
	va_start(args, fmt);
	vsnprintf(ctx->error->message, sizeof ctx->error->message, fmt, args);
	va_end(args);

	fz_flush_warnings(ctx);
	LOGE("error: %s\n", ctx->error->message);

	throw(ctx->error);
}

void fz_rethrow(fz_context *ctx)
{
	throw(ctx->error);
}
