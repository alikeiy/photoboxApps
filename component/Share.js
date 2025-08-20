import Share from 'react-native-share';

const shareToPrinter = async (photoPath) => {
  try {
    await Share.open({
      url: 'file://' + photoPath,
      type: 'image/jpeg',
    });
  } catch (err) {
    console.log(err);
  }
};