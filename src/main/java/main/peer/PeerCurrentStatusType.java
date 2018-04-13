package main.peer;

public enum PeerCurrentStatusType {
    HE_INTERESTED_IN_ME,
    HE_IS_NOT_INTERESTED_IN_ME,
    I_INTERESTED_IN_HIM,
    I_AM_NOT_INTERESTED_IN_HIM,
    HE_CHOKE_ME,
    HE_IS_NOT_CHOKE_ME,
    I_CHOKE_HIM,
    I_AM_NOT_CHOKE_HIM,
    I_AM_DOWNLOAD_FROM_HIM,
    I_AM_NOT_DOWNLOAD_FROM_HIM,
    I_UPLOAD_TO_HIM,
    I_AM_NOT_UPLOAD_TO_HIM,
    PIECES_STATUS_CHANGE
}
