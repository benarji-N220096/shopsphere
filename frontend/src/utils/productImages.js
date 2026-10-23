/**
 * Utility to reliably map products to high-resolution product imagery.
 * Guarantees that no product displays an empty or broken placeholder.
 */
export function getProductImage(product) {
  if (!product) return '/images/products/headphones.jpg';

  const img = product.imageUrl;
  // If a valid local image path or valid external non-example URL exists, use it
  if (img && typeof img === 'string') {
    if (img.startsWith('/') && !img.includes('example.com')) {
      return img;
    }
    if ((img.startsWith('http://') || img.startsWith('https://')) && !img.includes('example.com')) {
      return img;
    }
  }

  // Fallback intelligent mapping based on product name and category
  const name = (product.name || '').toLowerCase();
  const cat = (product.category || '').toLowerCase();

  if (name.includes('phone') || name.includes('mobile') || cat.includes('mobile')) {
    return '/images/products/smartphone.jpg';
  }
  if (name.includes('airpod')) {
    return '/images/products/airpods.jpg';
  }
  if (name.includes('neckband') || name.includes('bluetooth')) {
    return '/images/products/bluetooth_neckband.jpg';
  }
  if (name.includes('refrigerator') || name.includes('fridge') || cat.includes('appliance')) {
    if (name.includes('washing') || name.includes('washer')) {
      return '/images/products/washing_machine.jpg';
    }
    return '/images/products/refrigerator.jpg';
  }
  if (name.includes('washing') || name.includes('washer')) {
    return '/images/products/washing_machine.jpg';
  }
  if (name.includes('light') || name.includes('lamp') || name.includes('neon') || cat.includes('decor')) {
    return '/images/products/aesthetic_lights.jpg';
  }
  if (name.includes('powerbank') || name.includes('power bank') || name.includes('battery')) {
    return '/images/products/powerbank.jpg';
  }
  if (name.includes('luxury') || name.includes('chronograph')) {
    return '/images/products/luxury_watch.jpg';
  }
  if (name.includes('smart speaker') || name.includes('assistant') || name.includes('echo') || name.includes('nest')) {
    return '/images/products/smart_speaker.jpg';
  }

  if (name.includes('monitor') || cat.includes('monitor') || name.includes('screen') || name.includes('display')) {
    return '/images/products/monitor.jpg';
  }
  if (name.includes('keyboard') || cat.includes('keyboard') || name.includes('keychron')) {
    return '/images/products/keyboard.jpg';
  }
  if (name.includes('headphone') || cat.includes('headphone') || name.includes('headset')) {
    return '/images/products/headphones.jpg';
  }
  if (name.includes('mouse') || cat.includes('mouse') || name.includes('peripheral') || cat.includes('peripherals')) {
    return '/images/products/mouse.jpg';
  }
  if (name.includes('watch') || cat.includes('wearable') || name.includes('fitness') || name.includes('band')) {
    return '/images/products/smartwatch.jpg';
  }
  if (name.includes('speaker') || cat.includes('speaker') || name.includes('sound')) {
    return '/images/products/speaker.jpg';
  }
  if (name.includes('webcam') || cat.includes('webcam') || name.includes('camera') || name.includes('stream')) {
    return '/images/products/webcam.jpg';
  }
  if (name.includes('earbud') || name.includes('tws') || cat.includes('audio')) {
    return '/images/products/earbuds.jpg';
  }
  if (name.includes('stand') || name.includes('riser') || name.includes('holder')) {
    return '/images/products/laptop_stand.jpg';
  }
  if (name.includes('desk') || name.includes('pad') || name.includes('mat')) {
    return '/images/products/deskpad.jpg';
  }

  return '/images/products/headphones.jpg';
}
