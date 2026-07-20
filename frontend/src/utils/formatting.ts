/**
 * Utility functions for formatting
 */

export const formatting = {
  /**
   * Format timestamp to readable date/time
   */
  formatTime(timestamp: number): string {
    const date = new Date(timestamp);
    return date.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: true,
    });
  },

  /**
   * Format timestamp to date
   */
  formatDate(timestamp: number): string {
    const date = new Date(timestamp);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  },

  /**
   * Format large numbers with commas
   */
  formatNumber(num: number): string {
    return new Intl.NumberFormat('en-US').format(num);
  },

  /**
   * Format bytes to human readable
   */
  formatBytes(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  },

  /**
   * Format milliseconds to readable duration
   */
  formatDuration(ms: number): string {
    if (ms < 1000) return `${Math.round(ms)}ms`;
    if (ms < 60000) return `${(ms / 1000).toFixed(2)}s`;
    if (ms < 3600000) return `${(ms / 60000).toFixed(2)}m`;
    return `${(ms / 3600000).toFixed(2)}h`;
  },

  /**
   * Truncate string with ellipsis
   */
  truncate(str: string, length: number): string {
    return str.length > length ? `${str.substring(0, length)}...` : str;
  },

  /**
   * Format JSON for display
   */
  formatJSON(obj: any, indent: number = 2): string {
    return JSON.stringify(obj, null, indent);
  },
};
