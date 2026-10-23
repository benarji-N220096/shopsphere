import React from 'react';
import { CheckCircle, AlertTriangle, XCircle, CreditCard } from 'lucide-react';
import { Badge } from '../common/Badge';

export function PaymentStatusBadge({ status }) {
  const normStatus = (status || '').toUpperCase();

  switch (normStatus) {
    case 'SUCCESS':
      return (
        <Badge variant="success">
          <CheckCircle size={13} style={{ marginRight: '4px' }} />
          Paid
        </Badge>
      );
    case 'INITIATED':
      return (
        <Badge variant="primary">
          <CreditCard size={13} style={{ marginRight: '4px' }} />
          Initiated
        </Badge>
      );
    case 'FAILED':
      return (
        <Badge variant="error">
          <XCircle size={13} style={{ marginRight: '4px' }} />
          Failed
        </Badge>
      );
    default:
      return (
        <Badge variant="neutral">
          <AlertTriangle size={13} style={{ marginRight: '4px' }} />
          {normStatus || 'Unavailable'}
        </Badge>
      );
  }
}
