import { observeEdgeHandler } from '../_shared/observability.ts';
import { handleRequest } from './handler.ts';

Deno.serve(observeEdgeHandler('your-life-one-inventory-mcp', handleRequest));
