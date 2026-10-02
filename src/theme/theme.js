import {StyleSheet} from 'react-native';

export const colors = {
  background: '#FFF0F5',
  backgroundDeep: '#FFE4EE',
  card: '#FFFFFF',
  border: '#F7C6D4',
  primary: '#FF91AF',
  primaryDeep: '#F06A93',
  text: '#5A3D40',
  textMuted: '#8C6B70',
  white: '#FFFFFF',
  ok: '#E56B8A',
};

export const font = {
  regular: 'Fredoka-Regular',
  semibold: 'Fredoka-SemiBold',
  bold: 'Fredoka-Bold',
};

export const shadow = {
  shadowColor: '#E7A8BA',
  shadowOffset: {width: 0, height: 6},
  shadowOpacity: 0.28,
  shadowRadius: 12,
  elevation: 4,
};

export const ui = StyleSheet.create({
  screen: {
    flexGrow: 1,
    backgroundColor: colors.background,
    padding: 20,
    gap: 12,
  },
  title: {
    color: colors.text,
    fontFamily: font.bold,
    fontSize: 30,
  },
  body: {
    color: colors.textMuted,
    fontFamily: font.regular,
    fontSize: 15,
    lineHeight: 22,
  },
  card: {
    backgroundColor: colors.card,
    borderRadius: 24,
    borderWidth: 1,
    borderColor: colors.border,
    padding: 16,
    gap: 8,
    ...shadow,
  },
  button: {
    backgroundColor: colors.primary,
    borderRadius: 28,
    paddingVertical: 14,
    paddingHorizontal: 18,
    alignItems: 'center',
  },
  buttonDeep: {
    backgroundColor: colors.primaryDeep,
  },
  buttonQuiet: {
    backgroundColor: colors.white,
    borderWidth: 1,
    borderColor: colors.border,
  },
  buttonText: {
    color: colors.white,
    fontFamily: font.semibold,
    fontSize: 16,
    textAlign: 'center',
  },
  buttonTextQuiet: {
    color: colors.text,
  },
  buttonDisabled: {
    opacity: 0.45,
  },
});
