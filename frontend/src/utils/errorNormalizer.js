export function normalizeError(error) {
  if (!error) {
    return 'An unexpected error occurred. Please try again.';
  }

  // Network / connection error
  if (error.name === 'TypeError' && error.message.includes('fetch')) {
    return 'ShopSphere could not reach the server. Please verify your connection or try again later.';
  }

  // Backend structured error response
  if (error.message && typeof error.message === 'string') {
    if (error.status === 401) {
      return 'Please sign in to access this feature.';
    }
    if (error.status === 403) {
      return 'You do not have permission to perform this action.';
    }
    if (error.status === 404) {
      return error.message || 'The requested resource was not found.';
    }
    if (error.status === 409) {
      return error.message || 'A conflicting record already exists.';
    }
    if (error.status >= 500) {
      return 'Our servers encountered an issue processing your request. Please try again shortly.';
    }
    return error.message;
  }

  return 'An unexpected error occurred. Please try again.';
}
