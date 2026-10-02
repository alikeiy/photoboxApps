import React, {useMemo} from 'react';
import {StyleSheet, View} from 'react-native';
import qrcode from 'qrcode-generator';

/**
 * QR rendered from a pure JS matrix so the booth does not need another native view.
 */
export default function QrCode({value, size = 220}) {
  const rows = useMemo(() => {
    const qr = qrcode(0, 'M');
    qr.addData(value);
    qr.make();
    const count = qr.getModuleCount();
    const cell = size / count;
    const next = [];
    for (let row = 0; row < count; row += 1) {
      const cells = [];
      for (let col = 0; col < count; col += 1) {
        cells.push({
          key: `${row}-${col}`,
          dark: qr.isDark(row, col),
          cell,
        });
      }
      next.push({key: String(row), cells});
    }
    return next;
  }, [value, size]);

  return (
    <View style={styles.pad}>
      {rows.map(row => (
        <View key={row.key} style={styles.row}>
          {row.cells.map(cell => (
            <View
              key={cell.key}
              style={{
                width: cell.cell,
                height: cell.cell,
                backgroundColor: cell.dark ? '#111' : '#fff',
              }}
            />
          ))}
        </View>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  pad: {
    backgroundColor: '#fff',
    padding: 12,
    borderRadius: 24,
    borderWidth: 1,
    borderColor: '#F7C6D4',
  },
  row: {
    flexDirection: 'row',
  },
});
