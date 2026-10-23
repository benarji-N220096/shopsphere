import React from 'react';
import { CheckCircle2, Clock, XCircle, AlertCircle } from 'lucide-react';
import { Badge } from '../common/Badge';

export function OrderStatusBadge({ status }) {
  const normStatus = (status || '').toUpperCase();

  switch (normStatus) {
    case 'CONFIRMED':
      return (
        <Badge variant="success">
          <CheckCircle2 size={13} style={{ marginRight: '4px' }} />
          Confirmed
        </Badge>
      );
    case 'PENDING':
      return (
        <Badge variant="warning">
          <Clock size={13} style={{ marginRight: '4px' }} />
          Pending
        </Badge>
      );
    case 'CANCELLED':
      return (
        <Badge variant="error">
          <XCircle size={13} style={{ marginRight: '4px' }} />
          Cancelled
        </Badge>
      );
    default:
      return (
        <Badge variant="neutral">
          <AlertCircle size={13} style={{ marginRight: '4px' }} />
          {normStatus || 'Unknown'}
        </Badge>
      );
  }
}
